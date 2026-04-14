package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to confirm and enable a 2FA method setup.
 */
@Getter
@Setter
public class EnableTwoFactorMethodRequest {

    @Schema(description = "Setup token returned from start-setup endpoint", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "setupToken is required")
    private String setupToken;

    @Schema(description = "OTP code from authenticator app (TOTP) or email verification code (EMAIL)", example = "123456")
    @NotBlank(message = "otpCode is required")
    private String otpCode;
}
