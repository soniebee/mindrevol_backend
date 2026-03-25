package com.mindrevol.core.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response when starting setup of a new 2FA method
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class StartTwoFactorMethodSetupResponse {

    @Schema(description = "Method type being setup", example = "TOTP")
    private String method;

    @Schema(description = "QR code image in Base64 format (for TOTP method)", example = "data:image/png;base64,...")
    private String qrCode;

    @Schema(description = "OTP Auth URI for manual entry (for TOTP)", example = "otpauth://totp/MindRevol:user@example.com?secret=...")
    private String otpAuthUri;

    @Schema(description = "Manual secret for fallback (if revealSecret=true)")
    private String secret;

    @Schema(description = "Setup token to use in confirm endpoint", example = "uuid-token")
    private String setupToken;

    @Schema(description = "Message with next steps", example = "Scan QR code with authenticator app and verify with 6-digit code")
    private String message;

    @Schema(description = "Email masked (for EMAIL method)", example = "user***@example.com")
    private String email;
}
