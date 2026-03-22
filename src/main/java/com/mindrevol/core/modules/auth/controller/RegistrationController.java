package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.request.*;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.service.RegistrationService;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Registration", description = "APIs for registration and account activation")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/check-email")
    @Operation(summary = "Check email", description = "Check whether the email already exists.")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        UserSummaryResponse summary = registrationService.checkEmail(request.getEmail());
        if (summary != null) {
            return ResponseEntity.ok(ApiResponse.success(summary, "Email already exists"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Email is available"));
    }

    @PostMapping("/check-handle")
    @Operation(summary = "Check handle", description = "Check whether the user handle already exists.")
    public ResponseEntity<ApiResponse<Boolean>> checkHandle(@Valid @RequestBody CheckHandleRequest request) {
        boolean exists = registrationService.isHandleExists(request.getHandle());
        if (exists) {
            return ResponseEntity.ok(ApiResponse.success(true, "Handle is already taken"));
        }
        return ResponseEntity.ok(ApiResponse.success(false, "Handle is available"));
    }

    @PostMapping("/register")
    @Operation(summary = "Registration Step 1: Send OTP", description = "Submit information and send OTP to email.")
    public ResponseEntity<ApiResponse<Void>> registerStep1(@Valid @RequestBody RegisterRequest request) {
        registrationService.registerUserStep1(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code (OTP) has been sent to your email."));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Registration Step 2: Verify OTP", description = "Verify OTP and create account.")
    public ResponseEntity<ApiResponse<JwtResponse>> verifyRegisterOtp(
            @Valid @RequestBody VerifyRegisterOtpRequest request,
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = registrationService.verifyRegisterOtp(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "Registration successful!"));
    }

    @PostMapping("/register/resend")
    @Operation(summary = "Resend OTP", description = "Resend a new registration OTP.")
    public ResponseEntity<ApiResponse<Void>> resendRegisterOtp(@Valid @RequestBody ResendRegisterOtpRequest request) {
        registrationService.resendRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "A new verification code has been sent."));
    }

    @GetMapping("/activate")
    @Operation(summary = "Activate account (link)", description = "Account activation via email link.")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam("token") String token) {
        registrationService.activateUserAccount(token);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully!"));
    }
}