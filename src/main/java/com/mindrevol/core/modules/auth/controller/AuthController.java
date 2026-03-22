package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorBackupCodesResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorEnableResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorSetupResponse;
import com.mindrevol.core.modules.auth.dto.response.TwoFactorStatusResponse;
import com.mindrevol.core.modules.auth.service.AuthService;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Login", description = "Login methods")
public class AuthController {

    private final AuthService authService;

    // --- TRADITIONAL LOGIN ---

    @PostMapping("/login")
    @Operation(summary = "Standard login", description = "Email & Password")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.login(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    // --- SOCIAL LOGIN ---

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

    // --- OTP & MAGIC LINK LOGIN ---

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

    @PostMapping("/login/2fa/verify")
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

    // --- PROFILE ---

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/2fa/status")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get two-factor status")
    public ResponseEntity<ApiResponse<TwoFactorStatusResponse>> getTwoFactorStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(authService.getTwoFactorStatus(userDetails.getUsername())));
    }

    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Setup two-factor authentication", description = "Returns QR data for authenticator apps. Set revealSecret=true to return a manual secret as fallback.")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Return manual secret for users who cannot scan QR", example = "false")
            @RequestParam(defaultValue = "false") boolean revealSecret) {
        return ResponseEntity.ok(ApiResponse.success(authService.setupTwoFactor(userDetails.getUsername(), revealSecret)));
    }

    @PostMapping("/2fa/enable")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Enable two-factor authentication")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Verify setup by sending one TOTP code from authenticator app",
            content = @Content(
                    examples = @ExampleObject(value = "{\"otpCode\":\"123456\"}")
            )
    )
    public ResponseEntity<ApiResponse<TwoFactorEnableResponse>> enableTwoFactor(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorEnableRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.enableTwoFactor(userDetails.getUsername(), request)));
    }

    @PostMapping("/2fa/backup-codes/generate")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Generate backup codes", description = "Generate/regenerate backup codes in a separate step after 2FA is enabled")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "OTP confirmation is required before issuing new backup codes",
            content = @Content(
                    examples = @ExampleObject(value = "{\"otpCode\":\"123456\"}")
            )
    )
    public ResponseEntity<ApiResponse<TwoFactorBackupCodesResponse>> generateBackupCodes(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorGenerateBackupCodesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.generateTwoFactorBackupCodes(userDetails.getUsername(), request)));
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TwoFactorDisableRequest request) {
        authService.disableTwoFactor(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Two-factor authentication disabled."));
    }
}