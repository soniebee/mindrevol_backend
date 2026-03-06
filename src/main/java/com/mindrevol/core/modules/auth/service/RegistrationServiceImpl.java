package com.mindrevol.core.modules.auth.service;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.service.EmailService;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.*;
import com.mindrevol.core.modules.auth.mapper.AuthMapper;
import com.mindrevol.core.modules.auth.entity.RegisterTempData;
import com.mindrevol.core.common.utils.JwtUtil;
import com.mindrevol.core.modules.auth.repository.RegisterTempDataRepository;
import com.mindrevol.core.modules.user.entity.Role;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserStatus;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;

/**
 * Service Implementation xử lý các bước Registration Wizard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private final RegisterTempDataRepository registerTempDataRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    @Value("${app.registration.otp-validity-minutes:10}")
    private int otpValidityMinutes;

    @Value("${app.registration.otp-length:6}")
    private int otpLength;

    @Value("${app.mail.from:noreply@mindrevol.com}")
    private String mailFrom;

    @Value("${app.registration.temp-data-ttl-minutes:30}")
    private int tempDataTtlMinutes;

    @Override
    public AvailabilityResponse checkEmail(CheckEmailDto request) {
        log.debug("Checking email availability: {}", request.getEmail());
        boolean exists = userRepository.existsByEmail(request.getEmail());

        return AvailabilityResponse.builder()
                .available(!exists)
                .message(exists ? "Email này đã được đăng ký. Vui lòng sử dụng email khác" : "Email này khả dụng")
                .build();
    }

    @Override
    public AvailabilityResponse checkHandle(CheckHandleDto request) {
        log.debug("Checking handle availability: {}", request.getHandle());
        boolean exists = userRepository.existsByHandle(request.getHandle());

        return AvailabilityResponse.builder()
                .available(!exists)
                .message(exists ? "Handle này đã được sử dụng. Vui lòng chọn handle khác" : "Handle này khả dụng")
                .build();
    }

    @Override
    public void saveRegistrationStep1(RegisterStep1Dto request) {
        log.info("Processing registration step 1 for email: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu và xác nhận mật khẩu không trùng khớp");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email này đã tồn tại. Vui lòng sử dụng email khác");
        }

        RegisterTempData tempData = registerTempDataRepository
                .findByEmail(request.getEmail())
                .orElse(RegisterTempData.builder()
                        .id(request.getEmail())
                        .email(request.getEmail())
                        .build());

        authMapper.updateTempDataFromStep1(tempData, request);
        tempData.setCreatedAtMillis(System.currentTimeMillis());
        tempData.setUpdatedAtMillis(System.currentTimeMillis());
        tempData.setTtl((long) (tempDataTtlMinutes * 60));

        registerTempDataRepository.save(tempData);
        log.info("Step 1 saved for email: {}", request.getEmail());
    }

    @Override
    public void saveRegistrationStep2(String email, RegisterStep2Dto request) {
        log.info("Processing registration step 2 for email: {}", email);

        RegisterTempData tempData = registerTempDataRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu đăng ký tạm. Vui lòng bắt đầu lại từ bước 1"));

        if (userRepository.existsByHandle(request.getHandle())) {
            throw new BadRequestException("Handle này đã tồn tại. Vui lòng chọn handle khác");
        }

        authMapper.updateTempDataFromStep2(tempData, request);
        tempData.setUpdatedAtMillis(System.currentTimeMillis());
        tempData.setTtl((long) (tempDataTtlMinutes * 60));

        registerTempDataRepository.save(tempData);
        log.info("Step 2 saved for email: {}", email);
    }

    @Override
    public void saveRegistrationStep3(String email, RegisterStep3Dto request) {
        log.info("Processing registration step 3 for email: {}", email);

        RegisterTempData tempData = registerTempDataRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu đăng ký tạm. Vui lòng bắt đầu lại"));

        authMapper.updateTempDataFromStep3(tempData, request);
        tempData.setUpdatedAtMillis(System.currentTimeMillis());
        tempData.setTtl((long) (tempDataTtlMinutes * 60));

        registerTempDataRepository.save(tempData);
        log.info("Step 3 saved for email: {}", email);
    }

    private String generateOtpCode() {
        Random random = new Random();
        int otp = random.nextInt((int) Math.pow(10, otpLength));
        return String.format("%0" + otpLength + "d", otp);
    }

    @Override
    public void generateAndSendOtp(String email) {
        log.info("Generating and sending OTP for email: {}", email);

        RegisterTempData tempData = registerTempDataRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu đăng ký. Vui lòng hoàn thành bước 1-3 trước"));

        if (tempData.getEmail() == null || tempData.getPassword() == null ||
            tempData.getHandle() == null || tempData.getFullname() == null) {
            throw new BadRequestException("Vui lòng hoàn thành tất cả các bước trước khi xác nhận OTP");
        }

        String otpCode = generateOtpCode();
        long otpExpirationTime = System.currentTimeMillis() + (otpValidityMinutes * 60 * 1000);

        tempData.setOtpCode(otpCode);
        tempData.setOtpExpirationTime(otpExpirationTime);
        tempData.resetOtpAttempts();
        tempData.setUpdatedAtMillis(System.currentTimeMillis());
        tempData.setTtl((long) (tempDataTtlMinutes * 60));

        registerTempDataRepository.save(tempData);

        sendOtpEmail(email, tempData.getFullname(), otpCode);

        log.info("OTP sent successfully to email: {}", email);
    }

    private void sendOtpEmail(String email, String fullname, String otpCode) {
        try {
            String subject = "Mã xác thực (OTP) đăng ký tài khoản MindRevol";
            String htmlContent = buildOtpEmailContent(fullname, otpCode);

            emailService.sendHtmlEmail(email, subject, htmlContent);

            log.info("OTP email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new BadRequestException("Không thể gửi email. Vui lòng thử lại sau");
        }
    }

    private String buildOtpEmailContent(String fullname, String otpCode) {
        return "<html>" +
                "<body style=\"font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">" +
                "<h2 style=\"color: #333; text-align: center;\">Xác nhận đăng ký tài khoản</h2>" +
                "<p style=\"color: #666; font-size: 16px;\">Xin chào <strong>" + fullname + "</strong>,</p>" +
                "<p style=\"color: #666; font-size: 16px;\">Cảm ơn bạn đã đăng ký MindRevol. Vui lòng sử dụng mã xác thực dưới đây để hoàn tất quá trình đăng ký:</p>" +
                "<div style=\"text-align: center; margin: 30px 0;\">" +
                "<div style=\"display: inline-block; background-color: #007bff; color: white; padding: 15px 30px; border-radius: 5px; font-size: 24px; font-weight: bold; letter-spacing: 2px;\">" +
                otpCode +
                "</div>" +
                "</div>" +
                "<p style=\"color: #999; font-size: 14px;\">Mã này sẽ hết hạn sau " + otpValidityMinutes + " phút.</p>" +
                "<p style=\"color: #666; font-size: 14px;\">Nếu bạn không phải người thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px; text-align: center;\">© 2026 MindRevol. All rights reserved.</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void verifyOtp(OtpVerificationDto request) {
        log.info("Verifying OTP for email: {}", request.getEmail());

        RegisterTempData tempData = registerTempDataRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu đăng ký. Vui lòng bắt đầu lại từ bước 1"));

        if (tempData.isOtpExpired()) {
            tempData.resetOtpAttempts();
            tempData.setTtl((long) (tempDataTtlMinutes * 60));
            registerTempDataRepository.save(tempData);
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới");
        }

        if (tempData.isOtpAttemptsExceeded()) {
            throw new BadRequestException("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu mã mới");
        }

        if (!request.getOtp().equals(tempData.getOtpCode())) {
            tempData.incrementOtpAttempts();
            tempData.setTtl((long) (tempDataTtlMinutes * 60));
            registerTempDataRepository.save(tempData);

            int remainingAttempts = 3 - tempData.getOtpAttempts();
            throw new BadRequestException("Mã OTP không chính xác. Còn " + remainingAttempts + " lần thử");
        }

        tempData.setOtpCode(null);
        tempData.resetOtpAttempts();
        tempData.setTtl((long) (tempDataTtlMinutes * 60));
        registerTempDataRepository.save(tempData);

        log.info("OTP verified successfully for email: {}", request.getEmail());
    }

    @Override
    public void resendOtp(ResendOtpDto request) {
        log.info("Resending OTP for email: {}", request.getEmail());
        generateAndSendOtp(request.getEmail());
    }

    @Override
    public RegistrationResponse authenticate(LoginDto request) {
        log.info("Authenticating user: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Email hoặc mật khẩu không chính xác");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return authMapper.toRegistrationResponse(user, accessToken, refreshToken, "Đăng nhập thành công");
    }

    @Override
    public RegistrationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user);
        
        return authMapper.toRegistrationResponse(user, newAccessToken, refreshToken, "Làm mới token thành công");
    }

    @Override
    @Transactional
    public RegistrationResponse completeRegistration(String email) {
        log.info("Completing registration for email: {}", email);

        RegisterTempData tempData = registerTempDataRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu đăng ký. Vui lòng bắt đầu lại"));

        if (tempData.getOtpCode() != null) {
            throw new BadRequestException("Vui lòng xác thực OTP trước khi hoàn thành đăng ký");
        }

        if (tempData.getEmail() == null || tempData.getPassword() == null ||
            tempData.getHandle() == null || tempData.getFullname() == null) {
            throw new BadRequestException("Dữ liệu không đủ để tạo tài khoản. Vui lòng hoàn thành tất cả bước");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role USER trong hệ thống"));

        User newUser = User.builder()
                .email(tempData.getEmail())
                .password(passwordEncoder.encode(tempData.getPassword()))
                .handle(tempData.getHandle())
                .fullname(tempData.getFullname())
                .dateOfBirth(tempData.getDateOfBirth())
                .gender(tempData.getGender())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(java.util.Collections.singletonList(userRole)))
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user created with email: {}, ID: {}", savedUser.getEmail(), savedUser.getId());

        String accessToken = jwtUtil.generateAccessToken(savedUser);
        String refreshToken = jwtUtil.generateRefreshToken(savedUser);
        log.info("JWT token generated for user: {}", savedUser.getEmail());

        registerTempDataRepository.deleteById(email);
        log.info("Temporary registration data deleted for email: {}", email);

        return authMapper.toRegistrationResponse(savedUser, accessToken, refreshToken, "Đăng ký thành công! Bạn sẽ được chuyển hướng tới trang chủ");
    }

    @Override
    public RegisterTempData getRegistrationData(String email) {
        log.debug("Fetching registration data for email: {}", email);

        return registerTempDataRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu đăng ký"));
    }
}
