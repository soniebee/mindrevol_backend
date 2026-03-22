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
public class TwoFactorSetupResponse {

    @Schema(description = "TOTP URI used by authenticator apps", example = "otpauth://totp/MindRevol:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=MindRevol&algorithm=SHA1&digits=6&period=30")
    private String otpAuthUri;

    @Schema(description = "Base64 encoded PNG QR image for authenticator scan", example = "iVBORw0KGgoAAAANSUhEUgAA...")
    private String qrCode; // Base64-encoded PNG QR code image

    @Schema(description = "Manual setup secret (only returned when revealSecret=true)", example = "JBSWY3DPEHPK3PXP")
    private String secret;

    @Schema(description = "Whether the manual secret is included in this response", example = "false")
    private boolean manualSecretVisible;
}




