package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.*;
import com.mindrevol.core.modules.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý đăng ký người dùng
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API xác thực và đăng ký")
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Bước 1: Nhập email và handle
     */
    @PostMapping("/register/step1")
    @Operation(summary = "Đăng ký bước 1", description = "Nhập email và handle, kiểm tra trùng lặp")
    public ResponseEntity<ApiResponse<Void>> registerStep1(@Valid @RequestBody RegisterStep1Request request) {
        log.info("Registration Step 1 - Email: {}, Handle: {}", request.getEmail(), request.getHandle());
        registrationService.registerStep1(request);

        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Bước 1 hoàn tất. Vui lòng tiếp tục bước 2"
        ));
    }

    /**
     * Bước 2: Nhập thông tin cá nhân và mật khẩu, gửi OTP
     */
    @PostMapping("/register/step2")
    @Operation(summary = "Đăng ký bước 2", description = "Nhập thông tin cá nhân, mật khẩu và gửi OTP qua email")
    public ResponseEntity<ApiResponse<Void>> registerStep2(@Valid @RequestBody RegisterStep2Request request) {
        log.info("Registration Step 2 - Email: {}", request.getEmail());
        registrationService.registerStep2(request);

        return ResponseEntity.ok(ApiResponse.success(
            null,
            "OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư"
        ));
    }

    /**
     * Bước 3: Xác thực OTP và hoàn tất đăng ký
     */
    @PostMapping("/register/step3")
    @Operation(summary = "Đăng ký bước 3", description = "Xác thực OTP và hoàn tất đăng ký, trả về JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> registerStep3(@Valid @RequestBody RegisterStep3Request request) {
        log.info("Registration Step 3 - Email: {}", request.getEmail());
        AuthResponse authResponse = registrationService.registerStep3(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    authResponse,
                    "Đăng ký thành công! Chào mừng bạn đến với Mindrevol"
                ));
    }

    /**
     * Kiểm tra email hoặc handle có tồn tại không
     */
    @PostMapping("/check-availability")
    @Operation(summary = "Kiểm tra tính khả dụng", description = "Kiểm tra email hoặc handle đã tồn tại chưa")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkAvailability(
            @RequestBody CheckAvailabilityRequest request) {

        boolean available = registrationService.checkAvailability(request);

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("available", available),
            available ? "Có thể sử dụng" : "Đã tồn tại"
        ));
    }

    /**
     * Gửi lại mã OTP
     */
    @PostMapping("/resend-otp")
    @Operation(summary = "Gửi lại OTP", description = "Gửi lại mã OTP nếu người dùng chưa nhận được hoặc mã đã hết hạn")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Resend OTP - Email: {}", request.getEmail());
        registrationService.resendOtp(request);

        return ResponseEntity.ok(ApiResponse.success(
            null,
            "Mã OTP mới đã được gửi đến email của bạn"
        ));
    }
}

