package com.mindrevol.core.modules.auth.service.impl;

import com.mindrevol.core.modules.auth.service.impl.RegistrationServiceImpl;
import com.mindrevol.core.modules.user.entity.AccountType;
import com.mindrevol.core.modules.user.entity.Role;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserStatus;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.auth.dto.RegisterTempData;
import com.mindrevol.core.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.core.modules.auth.dto.request.ResendRegisterOtpRequest;
import com.mindrevol.core.modules.auth.dto.request.VerifyRegisterOtpRequest;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.entity.UserActivationToken;
import com.mindrevol.core.modules.auth.repository.UserActivationTokenRepository;
import com.mindrevol.core.modules.auth.service.RegistrationService;
import com.mindrevol.core.modules.auth.service.SessionService;
import com.mindrevol.core.modules.notification.dto.EmailTask;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.core.modules.user.entity.*;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserActivationTokenRepository activationTokenRepository;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AsyncTaskProducer asyncTaskProducer;
    private final PasswordEncoder passwordEncoder;
    
    private final SessionService sessionService; 

    private static final String REG_TEMP_PREFIX = "reg_temp:";
    private static final long REG_TEMP_TTL_MINUTES = 10;

    @Override
    public void registerUserStep1(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already used by another account.");
        }
        if (userRepository.existsByHandle(request.getHandle())) {
            throw new BadRequestException("Handle @" + request.getHandle() + " already exists.");
        }
        if (request.getDateOfBirth() == null) throw new BadRequestException("Date of birth is required.");

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        RegisterTempData tempData = RegisterTempData.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .handle(request.getHandle())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .otpCode(otpCode)
                .retryCount(0)
                .build();

        String redisKey = REG_TEMP_PREFIX + request.getEmail();
        redisTemplate.opsForValue().set(redisKey, tempData, REG_TEMP_TTL_MINUTES, TimeUnit.MINUTES);

        String subject = "MindRevol account registration verification";
        String content = "<div style='font-family: sans-serif; padding: 20px; color: #333;'>" +
                "<h2>Hello " + request.getFullname() + ",</h2>" +
                "<p>Thank you for registering with MindRevol.</p>" +
                "<p>Your verification code (OTP) is:</p>" +
                "<h1 style='color: #4F46E5; letter-spacing: 5px; background: #f3f4f6; display: inline-block; padding: 10px 20px; border-radius: 8px;'>" + otpCode + "</h1>" +
                "<p>This code is valid for <b>10 minutes</b>.</p>" +
                "<p style='font-size: 12px; color: #666;'>If you did not request this, please ignore this email.</p>" +
                "</div>";
        
        EmailTask task = EmailTask.builder()
                .toEmail(request.getEmail())
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build();
        asyncTaskProducer.submitEmailTask(task);

        log.info("Step 1 Register: OTP sent to temp user {}", request.getEmail());
    }

    @Override
    @Transactional
    public JwtResponse verifyRegisterOtp(VerifyRegisterOtpRequest request, HttpServletRequest servletRequest) {
        String redisKey = REG_TEMP_PREFIX + request.getEmail();
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);

        if (tempData == null) {
            throw new BadRequestException("Verification code has expired or email is incorrect. Please register again.");
        }

        if (!tempData.getOtpCode().equals(request.getOtpCode())) {
            int currentRetry = tempData.getRetryCount() + 1;
            tempData.setRetryCount(currentRetry);
            
            if (currentRetry > 5) {
                redisTemplate.delete(redisKey);
                throw new BadRequestException("You entered incorrect OTP too many times. Registration session has been canceled.");
            }
            
            redisTemplate.opsForValue().set(redisKey, tempData, REG_TEMP_TTL_MINUTES, TimeUnit.MINUTES);
            throw new BadRequestException("Incorrect OTP code. You have " + (6 - currentRetry) + " attempts left.");
        }

        if (userRepository.existsByHandle(tempData.getHandle())) {
            throw new BadRequestException("Sorry, handle @" + tempData.getHandle() + " has just been taken by another user. Please choose a different handle.");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("Default user").build()));

        User newUser = User.builder()
                .email(tempData.getEmail())
                .password(tempData.getPassword()) 
                .fullname(tempData.getFullname())
                .handle(tempData.getHandle())
                .dateOfBirth(tempData.getDateOfBirth())
                .gender(tempData.getGender())
                .status(UserStatus.ACTIVE)
                .accountType(AccountType.FREE)
                .authProvider("LOCAL")
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        userRepository.save(newUser);
        
        createDefaultSettings(newUser);

        redisTemplate.delete(redisKey);

        return sessionService.createTokenAndSession(newUser, servletRequest);
    }

    @Override
    public void resendRegisterOtp(ResendRegisterOtpRequest request) {
        String redisKey = REG_TEMP_PREFIX + request.getEmail();
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);

        if (tempData == null) {
            throw new BadRequestException("Registration session does not exist or has expired. Please register again.");
        }

        String newOtp = String.format("%06d", new Random().nextInt(999999));
        tempData.setOtpCode(newOtp);
        
        redisTemplate.opsForValue().set(redisKey, tempData, REG_TEMP_TTL_MINUTES, TimeUnit.MINUTES);

        String subject = "Resend verification code - MindRevol";
        String content = "<p>Your NEW verification code is: <b style='font-size: 20px; color: #4F46E5;'>" + newOtp + "</b></p>";
        
        EmailTask task = EmailTask.builder()
                .toEmail(tempData.getEmail())
                .subject(subject)
                .content(content)
                .retryCount(0)
                .build();
        asyncTaskProducer.submitEmailTask(task);
    }

    @Override
    public UserSummaryResponse checkEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean hasPass = "LOCAL".equalsIgnoreCase(user.getAuthProvider());
            
            return UserSummaryResponse.builder()
                    .id(user.getId())
                    .fullname(user.getFullname())
                    .handle(user.getHandle())
                    .avatarUrl(user.getAvatarUrl())
                    .isOnline(true)
                    .hasPassword(hasPass)
                    .authProvider(user.getAuthProvider()) 
                    .build();
        }
        return null;
    }

    @Override
    public boolean isHandleExists(String handle) {
        return userRepository.existsByHandle(handle);
    }

    @Override
    public void activateUserAccount(String token) {
        UserActivationToken activationToken = activationTokenRepository.findByToken(token).orElseThrow(() -> new BadRequestException("Invalid token."));
        if (activationToken.isExpired()) {
            activationTokenRepository.delete(activationToken);
            throw new BadRequestException("Token has expired.");
        }
        User user = activationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);
    }

    private void createDefaultSettings(User user) {
        try {
             // userService.createDefaultSettings(user); 
        } catch (Exception e) {
            log.warn("Could not create default settings for user {}", user.getId());
        }
    }
}