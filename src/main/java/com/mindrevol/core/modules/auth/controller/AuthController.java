package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.ConfirmTwoFactorMethodResponse;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.StartTwoFactorMethodSetupResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorBackupCodesResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorMethodStatusResponse;
import com.mindrevol.core.modules.auth.service.AuthService;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Login", description = "Login methods")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Standard login", description = "Email & Password")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.login(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<JwtResponse>> loginGoogle(@RequestBody GoogleLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithGoogle(request, servletRequest)));
    }

    @PostMapping("/login/apple")
    public ResponseEntity<ApiResponse<JwtResponse>> loginApple(@RequestBody AppleLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithApple(request, servletRequest)));
    }

    @PostMapping("/login/facebook")
    public ResponseEntity<ApiResponse<JwtResponse>> loginFacebook(@RequestBody FacebookLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithFacebook(request, servletRequest)));
    }

    @PostMapping("/login/tiktok")
    public ResponseEntity<ApiResponse<JwtResponse>> loginTikTok(@RequestBody TikTokLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithTikTok(request, servletRequest)));
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send login OTP")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtpLogin(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code has been sent to your email."));
    }

    @PostMapping("/otp/login")
    @Operation(summary = "Login with OTP")
    public ResponseEntity<ApiResponse<JwtResponse>> loginWithOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.verifyOtpLogin(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/2fa/methods/verify-login")
    @Operation(summary = "Verify two-factor login", description = "Complete second step and receive JWT")
    public ResponseEntity<ApiResponse<JwtResponse>> verifyTwoFactorLogin(
            @Valid @RequestBody TwoFactorLoginVerifyRequest request,
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.verifyTwoFactorLogin(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/magic-link")
    @Operation(summary = "Send Magic Link")
    public ResponseEntity<ApiResponse<Void>> sendMagicLink(@RequestBody ForgotPasswordRequest request) {
        authService.sendMagicLink(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Magic link has been sent to your email."));
    }

    @PostMapping("/magic-login")
    @Operation(summary = "Login with Magic Link")
    public ResponseEntity<ApiResponse<JwtResponse>> loginWithMagicLink(@RequestParam("token") String token, HttpServletRequest request) {
        JwtResponse jwtResponse = authService.loginWithMagicLink(token, request);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/2fa/backup-codes/generate")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Generate backup codes", description = "Generate/regenerate backup codes after TOTP is enabled")
    public ResponseEntity<ApiResponse<TwoFactorBackupCodesResponse>> generateBackupCodes(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorGenerateBackupCodesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.generateTwoFactorBackupCodes(userDetails.getUsername(), request)));
    }

    @GetMapping("/2fa/backup-codes/download")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Download latest backup codes", description = "Downloads the latest generated backup codes text file")
    public ResponseEntity<byte[]> downloadBackupCodes(@AuthenticationPrincipal UserDetails userDetails) {
        String content = authService.downloadTwoFactorBackupCodes(userDetails.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup-codes.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/2fa/methods/start-setup")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Start setup of a new 2FA method")
    public ResponseEntity<ApiResponse<StartTwoFactorMethodSetupResponse>> startTwoFactorMethodSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StartTwoFactorMethodSetupRequest request,
            @Parameter(description = "Return manual secret for TOTP (default: false)")
            @RequestParam(defaultValue = "false") boolean revealSecret) {
        return ResponseEntity.ok(ApiResponse.success(authService.startTwoFactorMethodSetup(userDetails.getUsername(), request, revealSecret)));
    }

    @PostMapping("/2fa/methods/confirm-setup")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Confirm and enable a 2FA method")
    public ResponseEntity<ApiResponse<ConfirmTwoFactorMethodResponse>> confirmTwoFactorMethodSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EnableTwoFactorMethodRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.confirmTwoFactorMethodSetup(userDetails.getUsername(), request)));
    }

    @PostMapping("/2fa/methods/disable")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Disable a specific 2FA method")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactorMethod(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DisableTwoFactorMethodRequest request) {
        authService.disableTwoFactorMethod(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Two-factor method disabled successfully."));
    }

    @GetMapping("/2fa/methods")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get detailed status of all 2FA methods")
    public ResponseEntity<ApiResponse<List<TwoFactorMethodStatusResponse>>> getTwoFactorMethods(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(authService.getTwoFactorMethods(userDetails.getUsername())));
    }

}