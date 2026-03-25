package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to disable a specific 2FA method
 */
@Getter
@Setter
public class DisableTwoFactorMethodRequest {

    @Schema(description = "2FA method type to disable", example = "TOTP", allowableValues = {"TOTP", "EMAIL", "BACKUP_CODES"})
    @NotBlank(message = "Method must not be blank")
    private String method; // "TOTP", "EMAIL", or "BACKUP_CODES"

    @Schema(description = "OTP code from any enabled method to confirm disabling", example = "123456")
    private String otpCode;

    @Schema(description = "Backup code to confirm disabling (alternative to otpCode)", example = "ABCD-1234")
    private String backupCode;
}
