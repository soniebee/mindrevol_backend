package com.mindrevol.core.modules.auth.service;

import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.*;
import com.mindrevol.core.modules.auth.entity.RegisterTempData;

/**
 * Interface Service xử lý các bước Registration Wizard
 */
public interface RegistrationService {

    /**
     * Kiểm tra email đã tồn tại trong hệ thống chưa
     */
    AvailabilityResponse checkEmail(CheckEmailDto request);

    /**
     * Kiểm tra handle đã tồn tại trong hệ thống chưa
     */
    AvailabilityResponse checkHandle(CheckHandleDto request);

    /**
     * Lưu dữ liệu bước 1 vào Redis
     */
    void saveRegistrationStep1(RegisterStep1Dto request);

    /**
     * Lưu dữ liệu bước 2 vào Redis
     */
    void saveRegistrationStep2(String email, RegisterStep2Dto request);

    /**
     * Lưu dữ liệu bước 3 vào Redis
     */
    void saveRegistrationStep3(String email, RegisterStep3Dto request);

    /**
     * Tạo và gửi OTP đến email người dùng
     */
    void generateAndSendOtp(String email);

    /**
     * Xác thực mã OTP nhập vào
     */
    void verifyOtp(OtpVerificationDto request);

    /**
     * Gửi lại mã OTP
     */
    void resendOtp(ResendOtpDto request);

    /**
     * Xử lý đăng nhập
     */
    RegistrationResponse authenticate(LoginDto request);

    /**
     * Làm mới Access Token bằng Refresh Token
     */
    RegistrationResponse refreshToken(RefreshTokenRequest request);

    /**
     * Hoàn thành đăng ký - Tạo User chính thức trong DB
     */
    RegistrationResponse completeRegistration(String email);

    /**
     * Lấy dữ liệu đăng ký hiện tại (dùng cho Wizard Back/Next)
     */
    RegisterTempData getRegistrationData(String email);
}
