package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.GoogleLoginDto;
import com.mindrevol.core.modules.auth.dto.SocialLoginResponse;
import com.mindrevol.core.modules.auth.service.SocialLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý Social Login (Google, Apple, Facebook, etc.)
 *
 * Endpoints:
 * - POST /api/v1/auth/login/google - Google login
 * - (Sau này: POST /api/v1/auth/login/apple, /facebook, etc.)
 */
@RestController
@RequestMapping("/api/v1/auth/login")
@RequiredArgsConstructor
@Slf4j
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    /**
     * POST /api/v1/auth/login/google
     *
     * Frontend flow:
     * 1. Google Identity SDK -> Lấy idToken
     * 2. POST idToken tới endpoint này
     * 3. Backend verify & trả JWT
     * 4. Frontend lưu JWT -> Navigate to home
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<SocialLoginResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginDto request) {
        log.info("Google login request received");

        try {
            SocialLoginResponse response = socialLoginService.loginWithGoogle(request);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    response.getIsNewUser() ? "Account created and logged in successfully" : "Logged in successfully"
            ));
        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Sau này thêm endpoints:
    // @PostMapping("/apple")
    // @PostMapping("/facebook")
    // @PostMapping("/tiktok")
}

