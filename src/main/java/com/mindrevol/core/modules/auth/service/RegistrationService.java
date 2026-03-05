package com.mindrevol.core.modules.auth.service;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.service.EmailService;
import com.mindrevol.core.modules.auth.dto.*;
import com.mindrevol.core.modules.auth.entity.RegisterTempData;
import com.mindrevol.core.modules.user.entity.*;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service xử lý đăng ký người dùng với OTP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    private static final String REGISTER_TEMP_KEY_PREFIX = "register:temp:";
    private static final long OTP_EXPIRATION_MINUTES = 15;
    private static final int MAX_OTP_ATTEMPTS = 5;

    /**
     * Bước 1: Kiểm tra email và handle, lưu tạm vào Redis
     */
    public void registerStep1(RegisterStep1Request request) {
        String email = request.getEmail().toLowerCase().trim();
        String handle = request.getHandle().toLowerCase().trim();

        // Kiểm tra trùng lặp
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
        }
        if (userRepository.existsByHandle(handle)) {
            throw new BadRequestException("Handle đã được sử dụng");
        }

        // Lưu tạm vào Redis (chỉ email và handle)
        String redisKey = REGISTER_TEMP_KEY_PREFIX + email;
        RegisterTempData tempData = RegisterTempData.builder()
                .email(email)
                .handle(handle)
                .createdAt(System.currentTimeMillis())
                .otpAttempts(0)
                .build();

        redisTemplate.opsForValue().set(redisKey, tempData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        log.info("Step 1 completed for email: {}", email);
    }

    /**
     * Bước 2: Lưu thông tin cá nhân, mã hóa mật khẩu, tạo và gửi OTP
     */
    public void registerStep2(RegisterStep2Request request) {
        String email = request.getEmail().toLowerCase().trim();
        String redisKey = REGISTER_TEMP_KEY_PREFIX + email;

        // Lấy dữ liệu tạm từ Redis
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);
        if (tempData == null) {
            throw new BadRequestException("Phiên đăng ký đã hết hạn. Vui lòng thử lại từ đầu");
        }

        // Cập nhật thông tin
        tempData.setFullname(request.getFullname());
        tempData.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa mật khẩu
        tempData.setDateOfBirth(request.getDateOfBirth());
        tempData.setGender(request.getGender());
        tempData.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");

        // Tạo mã OTP 6 số
        String otpCode = generateOTP();
        tempData.setOtpCode(otpCode);
        tempData.setOtpAttempts(0); // Reset số lần thử

        // Lưu lại vào Redis
        redisTemplate.opsForValue().set(redisKey, tempData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Gửi OTP qua email (async)
        emailService.sendOtpEmail(email, otpCode, request.getFullname());

        log.info("Step 2 completed for email: {}. OTP sent.", email);
    }

    /**
     * Bước 3: Xác thực OTP và tạo User chính thức
     */
    @Transactional
    public AuthResponse registerStep3(RegisterStep3Request request) {
        String email = request.getEmail().toLowerCase().trim();
        String redisKey = REGISTER_TEMP_KEY_PREFIX + email;

        // Lấy dữ liệu tạm từ Redis
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);
        if (tempData == null) {
            throw new BadRequestException("Phiên đăng ký đã hết hạn. Vui lòng thử lại từ đầu");
        }

        // Kiểm tra số lần thử
        if (tempData.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            redisTemplate.delete(redisKey);
            throw new BadRequestException("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng đăng ký lại");
        }

        // Kiểm tra OTP
        if (!request.getOtpCode().equals(tempData.getOtpCode())) {
            tempData.setOtpAttempts(tempData.getOtpAttempts() + 1);
            redisTemplate.opsForValue().set(redisKey, tempData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

            int remainingAttempts = MAX_OTP_ATTEMPTS - tempData.getOtpAttempts();
            throw new BadRequestException("Mã OTP không chính xác. Còn " + remainingAttempts + " lần thử");
        }

        // OTP đúng -> Tạo User
        User newUser = createUserFromTempData(tempData);
        userRepository.save(newUser);

        // Xóa dữ liệu tạm trong Redis
        redisTemplate.delete(redisKey);

        // Tạo JWT Token
        String accessToken = jwtService.generateAccessToken(newUser);
        String refreshToken = jwtService.generateRefreshToken(newUser);

        log.info("User registered successfully: {}", email);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .user(mapToUserDto(newUser))
                .build();
    }

    /**
     * Kiểm tra email hoặc handle có tồn tại không
     */
    public boolean checkAvailability(CheckAvailabilityRequest request) {
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            return !userRepository.existsByEmail(request.getEmail().toLowerCase().trim());
        }
        if (request.getHandle() != null && !request.getHandle().isEmpty()) {
            return !userRepository.existsByHandle(request.getHandle().toLowerCase().trim());
        }
        return true;
    }

    /**
     * Gửi lại OTP
     */
    public void resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String redisKey = REGISTER_TEMP_KEY_PREFIX + email;

        // Lấy dữ liệu tạm từ Redis
        RegisterTempData tempData = (RegisterTempData) redisTemplate.opsForValue().get(redisKey);
        if (tempData == null) {
            throw new BadRequestException("Phiên đăng ký đã hết hạn. Vui lòng thử lại từ đầu");
        }

        // Tạo OTP mới
        String newOtpCode = generateOTP();
        tempData.setOtpCode(newOtpCode);
        tempData.setOtpAttempts(0); // Reset số lần thử

        // Lưu lại vào Redis
        redisTemplate.opsForValue().set(redisKey, tempData, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Gửi OTP mới qua email
        emailService.sendOtpEmail(email, newOtpCode, tempData.getFullname());

        log.info("OTP resent to email: {}", email);
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Tạo mã OTP 6 số ngẫu nhiên
     */
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6 chữ số
        return String.valueOf(otp);
    }

    /**
     * Tạo User từ dữ liệu tạm
     */
    private User createUserFromTempData(RegisterTempData tempData) {
        // Lấy role USER mặc định
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role USER not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User.UserBuilder userBuilder = User.builder()
                .email(tempData.getEmail())
                .handle(tempData.getHandle())
                .fullname(tempData.getFullname())
                .password(tempData.getPassword()) // Đã mã hóa
                .status(UserStatus.ACTIVE) // Kích hoạt luôn sau khi xác thực OTP
                .accountType(AccountType.FREE)
                .authProvider("LOCAL")
                .timezone(tempData.getTimezone())
                .roles(roles);

        // Parse dateOfBirth nếu có
        if (tempData.getDateOfBirth() != null && !tempData.getDateOfBirth().isEmpty()) {
            try {
                userBuilder.dateOfBirth(LocalDate.parse(tempData.getDateOfBirth()));
            } catch (Exception e) {
                log.warn("Invalid date format for dateOfBirth: {}", tempData.getDateOfBirth());
            }
        }

        // Parse gender nếu có
        if (tempData.getGender() != null && !tempData.getGender().isEmpty()) {
            try {
                userBuilder.gender(Gender.valueOf(tempData.getGender().toUpperCase()));
            } catch (Exception e) {
                log.warn("Invalid gender value: {}", tempData.getGender());
            }
        }

        return userBuilder.build();
    }

    /**
     * Map User entity sang UserDto
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .handle(user.getHandle())
                .fullname(user.getFullname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .accountType(user.getAccountType() != null ? user.getAccountType().name() : null)
                .timezone(user.getTimezone())
                .build();
    }
}

