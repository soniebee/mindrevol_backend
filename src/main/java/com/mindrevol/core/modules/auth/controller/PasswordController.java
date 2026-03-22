package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth: Password", description = "Password and security management")
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset link to email.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link has been sent to email."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Change password using forgot-password token.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been changed successfully."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Change password", description = "For authenticated users.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        passwordService.changePassword(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully."));
    }

    @GetMapping("/has-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Check password existence", description = "For social-login users.")
    public ResponseEntity<ApiResponse<Boolean>> checkHasPassword(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(passwordService.hasPassword(userDetails.getUsername())));
    }

    @PostMapping("/create-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create password", description = "For social-login users setting password for the first time.")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @Valid @RequestBody CreatePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        passwordService.createPassword(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Password has been set successfully."));
    }

    @PostMapping("/update-password-otp")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update password with OTP", description = "Higher-security password change flow.")
    public ResponseEntity<ApiResponse<Void>> updatePasswordWithOtp(
            @Valid @RequestBody UpdatePasswordOtpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        passwordService.updatePasswordWithOtp(userDetails.getUsername(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully!"));
    }
}
