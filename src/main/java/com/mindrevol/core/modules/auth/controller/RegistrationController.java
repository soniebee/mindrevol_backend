package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.*;
import com.mindrevol.core.modules.auth.entity.RegisterTempData;
import com.mindrevol.core.modules.auth.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API liên quan đến Registration Wizard
 *
 * Endpoints:
 * - POST /api/v1/auth/register/step1 - Lưu bước 1 (Email + Password)
 * - POST /api/v1/auth/register/step2 - Lưu bước 2 (Handle + Fullname)
 * - POST /api/v1/auth/register/step3 - Lưu bước 3 (Ngày sinh + Giới tính)
 * - GET  /api/v1/auth/register/data   - Lấy dữ liệu đã nhập
 * - POST /api/v1/auth/check-email     - Check email available (Debounce)
 * - POST /api/v1/auth/check-handle    - Check handle available (Debounce)
 * - POST /api/v1/auth/send-otp        - Gửi OTP
 * - POST /api/v1/auth/verify-otp      - Xác thực OTP
 * - POST /api/v1/auth/resend-otp      - Gửi lại OTP
 * - POST /api/v1/auth/complete        - Hoàn thành đăng ký (tạo User)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegistrationService registrationService;

    // =====================================================
    // STEP 1: Lưu Email + Password
    // =====================================================

    /**
     * POST /api/v1/auth/register/step1
     * Lưu dữ liệu bước 1 (Email + Password)
     */
    @PostMapping("/register/step1")
    public ResponseEntity<ApiResponse<Void>> registerStep1(
            @Valid @RequestBody RegisterStep1Dto request) {
        log.info("Registering step 1 for email: {}", request.getEmail());

        registrationService.saveRegistrationStep1(request);

        return ResponseEntity.ok(ApiResponse.success("Bước 1 lưu thành công"));
    }

    // =====================================================
    // STEP 2: Lưu Handle + Fullname
    // =====================================================

    /**
     * POST /api/v1/auth/register/step2
     * Lưu dữ liệu bước 2 (Handle + Fullname)
     *
     * Body: RegisterStep2Dto
     * Query: email (của người dùng)
     */
    @PostMapping("/register/step2")
    public ResponseEntity<ApiResponse<Void>> registerStep2(
            @RequestParam String email,
            @Valid @RequestBody RegisterStep2Dto request) {
        log.info("Registering step 2 for email: {}", email);

        registrationService.saveRegistrationStep2(email, request);

        return ResponseEntity.ok(ApiResponse.success("Bước 2 lưu thành công"));
    }

    // =====================================================
    // STEP 3: Lưu Ngày sinh + Giới tính
    // =====================================================

    /**
     * POST /api/v1/auth/register/step3
     * Lưu dữ liệu bước 3 (Ngày sinh + Giới tính) - Optional
     *
     * Body: RegisterStep3Dto
     * Query: email (của người dùng)
     */
    @PostMapping("/register/step3")
    public ResponseEntity<ApiResponse<Void>> registerStep3(
            @RequestParam String email,
            @Valid @RequestBody RegisterStep3Dto request) {
        log.info("Registering step 3 for email: {}", email);

        registrationService.saveRegistrationStep3(email, request);

        return ResponseEntity.ok(ApiResponse.success("Bước 3 lưu thành công"));
    }

    // =====================================================
    // GET REGISTRATION DATA: Lấy dữ liệu đã nhập
    // =====================================================

    /**
     * GET /api/v1/auth/register/data?email=xxx@xxx.com
     * Lấy dữ liệu đăng ký hiện tại (dùng cho Wizard Back/Next)
     */
    @GetMapping("/register/data")
    public ResponseEntity<ApiResponse<RegisterTempData>> getRegistrationData(
            @RequestParam String email) {
        log.info("Fetching registration data for email: {}", email);

        RegisterTempData data = registrationService.getRegistrationData(email);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // =====================================================
    // CHECK EMAIL: Kiểm tra email available (Debounce)
    // =====================================================

    /**
     * POST /api/v1/auth/check-email
     * Kiểm tra email đã tồn tại chưa (dùng cho debounce)
     */
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkEmail(
            @Valid @RequestBody CheckEmailDto request) {
        log.debug("Checking email availability: {}", request.getEmail());

        AvailabilityResponse response = registrationService.checkEmail(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // CHECK HANDLE: Kiểm tra handle available (Debounce)
    // =====================================================

    /**
     * POST /api/v1/auth/check-handle
     * Kiểm tra handle đã tồn tại chưa (dùng cho debounce)
     */
    @PostMapping("/check-handle")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkHandle(
            @Valid @RequestBody CheckHandleDto request) {
        log.debug("Checking handle availability: {}", request.getHandle());

        AvailabilityResponse response = registrationService.checkHandle(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // SEND OTP: Gửi OTP đến email
    // =====================================================

    /**
     * POST /api/v1/auth/send-otp
     * Gửi OTP đến email người dùng
     *
     * Query: email
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @RequestParam String email) {
        log.info("Sending OTP to email: {}", email);

        registrationService.generateAndSendOtp(email);

        return ResponseEntity.ok(ApiResponse.success("OTP đã được gửi tới email. Vui lòng kiểm tra hộp thư của bạn"));
    }

    // =====================================================
    // VERIFY OTP: Xác thực OTP
    // =====================================================

    /**
     * POST /api/v1/auth/verify-otp
     * Xác thực mã OTP nhập vào
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody OtpVerificationDto request) {
        log.info("Verifying OTP for email: {}", request.getEmail());

        registrationService.verifyOtp(request);

        return ResponseEntity.ok(ApiResponse.success("OTP xác thực thành công"));
    }

    // =====================================================
    // RESEND OTP: Gửi lại OTP
    // =====================================================

    /**
     * POST /api/v1/auth/resend-otp
     * Gửi lại mã OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpDto request) {
        log.info("Resending OTP to email: {}", request.getEmail());

        registrationService.resendOtp(request);

        return ResponseEntity.ok(ApiResponse.success("OTP mới đã được gửi tới email của bạn"));
    }

    // =====================================================
    // LOGIN: Đăng nhập
    // =====================================================

    /**
     * POST /api/v1/auth/login
     * Đăng nhập hệ thống
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<RegistrationResponse>> login(
            @Valid @RequestBody LoginDto request) {
        log.info("User login attempt: {}", request.getEmail());

        RegistrationResponse response = registrationService.authenticate(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // REFRESH TOKEN: Làm mới Access Token
    // =====================================================

    /**
     * POST /api/v1/auth/refresh-token
     * Làm mới Access Token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RegistrationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");

        RegistrationResponse response = registrationService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // COMPLETE REGISTRATION: Hoàn thành đăng ký
    // =====================================================

    /**
     * POST /api/v1/auth/complete
     * Hoàn thành đăng ký - Tạo User chính thức trong DB
     *
     * Query: email
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<RegistrationResponse>> completeRegistration(
            @RequestParam String email) {
        log.info("Completing registration for email: {}", email);

        RegistrationResponse response = registrationService.completeRegistration(email);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }
}

