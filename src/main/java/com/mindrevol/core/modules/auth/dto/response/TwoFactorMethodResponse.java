package com.mindrevol.core.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TwoFactorMethodResponse {

    @Schema(description = "2FA method type", example = "TOTP")
    private String method;

    @Schema(description = "Status message", example = "Verification email sent to user@example.com")
    private String message;

    @Schema(description = "Setup URL (for TOTP, this contains the QR code generation URL)", example = "otpauth://totp/MindRevol:user@example.com?secret=JBSWY3DPEHPK3PXP")
    private String setupUrl;

    @Schema(description = "Verification token (returned for EMAIL method, used for verification)", example = "token123...")
    private String verificationToken;

    @Schema(description = "Backend URL to retrieve QR code image", example = "/api/v1/auth/2fa/setup?revealSecret=true")
    private String qrCodeUrl;
}
