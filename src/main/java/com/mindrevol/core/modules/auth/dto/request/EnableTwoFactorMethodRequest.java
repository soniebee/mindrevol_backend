package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to enable a specific 2FA method
 */
@Getter
@Setter
public class EnableTwoFactorMethodRequest {

    @Schema(description = "Setup token returned from start-setup endpoint", example = "550e8400-e29b-41d4-a716-446655440000")
    // Optional to keep backward compatibility with old app-based TOTP flow.
    private String setupToken;

    @Schema(description = "2FA method type to enable (optional, defaults to TOTP)", example = "TOTP", allowableValues = {"TOTP", "EMAIL"})
    private String method; // "TOTP" or "EMAIL"

    @Schema(description = "Email address for EMAIL OTP method (required if method=EMAIL)", example = "user@example.com")
    @Email(message = "Email must be valid")
    private String email; // Required if method=EMAIL

    @Schema(description = "OTP code for TOTP confirm, or email verification token alias for EMAIL confirm", example = "123456")
    private String otpCode; // Required for TOTP confirmation
}
