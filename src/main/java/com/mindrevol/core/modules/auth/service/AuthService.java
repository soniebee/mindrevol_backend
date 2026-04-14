package com.mindrevol.core.modules.auth.service;

import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.ConfirmTwoFactorMethodResponse;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.StartTwoFactorMethodSetupResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorBackupCodesResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorMethodStatusResponse;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    // Login truyền thống
    JwtResponse login(LoginRequest request, HttpServletRequest servletRequest);

    // Login Social
    JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest);
    JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest);
    JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest);
    JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest);

    // Get current profile (Có thể chuyển sang UserService, nhưng để tạm đây cũng được)
    UserProfileResponse getCurrentUserProfile(String userEmail);
    
    // Login bằng Magic Link (Link đăng nhập 1 lần gửi qua email)
    void sendMagicLink(String email);
    JwtResponse loginWithMagicLink(String token, HttpServletRequest request);

    // Login bằng OTP (Đăng nhập nhanh không cần pass)
    void sendOtpLogin(SendOtpRequest request);
    JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest);

    JwtResponse verifyTwoFactorLogin(TwoFactorLoginVerifyRequest request, HttpServletRequest servletRequest);
    
    // --- NEW METHODS FOR MULTIPLE 2FA METHODS SUPPORT ---
    
    /**
     * Start setup of a new 2FA method
     */
    StartTwoFactorMethodSetupResponse startTwoFactorMethodSetup(String userEmail, StartTwoFactorMethodSetupRequest request, boolean revealSecret);
    
    /**
     * Confirm/enable a new 2FA method
     */
    ConfirmTwoFactorMethodResponse confirmTwoFactorMethodSetup(String userEmail, EnableTwoFactorMethodRequest request);
    
    /**
     * Disable a specific 2FA method while keeping others enabled
     */
    void disableTwoFactorMethod(String userEmail, DisableTwoFactorMethodRequest request);
    
    /**
     * Get detailed status of all 2FA methods for the user
     */
    java.util.List<TwoFactorMethodStatusResponse> getTwoFactorMethods(String userEmail);
    
    TwoFactorBackupCodesResponse generateTwoFactorBackupCodes(String userEmail, TwoFactorGenerateBackupCodesRequest request);
    String downloadTwoFactorBackupCodes(String userEmail);
}

