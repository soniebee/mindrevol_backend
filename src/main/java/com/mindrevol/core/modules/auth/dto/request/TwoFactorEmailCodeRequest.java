package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorEmailCodeRequest {

    @Schema(description = "2FA challenge ID from login response", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Challenge ID must not be blank")
    private String challengeId;

    @Schema(description = "6-digit code sent to user's email", example = "123456")
    @NotBlank(message = "Email code must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "Email code must be 6 digits")
    private String emailCode;

    @Schema(description = "Device ID for session tracking")
    @Size(max = 255, message = "deviceId must be <= 255 chars")
    private String deviceId;

    @Schema(description = "Remember this device for future logins")
    private Boolean rememberDevice;
}

