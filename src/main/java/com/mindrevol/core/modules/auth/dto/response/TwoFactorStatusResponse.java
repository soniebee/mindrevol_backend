package com.mindrevol.core.modules.auth.dto.response;

import com.mindrevol.core.modules.auth.dto.BackupCodeStatusDto;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TwoFactorStatusResponse {

    @Schema(description = "Whether 2FA is enabled (any method)", example = "true")
    private boolean enabled;

    @Schema(description = "2FA method type (TOTP or EMAIL) - DEPRECATED: use individual method status instead", example = "TOTP")
    private String method;

    @Schema(description = "Email address for 2FA delivery (masked if EMAIL method) - DEPRECATED", example = "user***@example.com")
    private String email;

    @Schema(description = "Number of remaining backup codes - DEPRECATED", example = "5")
    private int backupCodesLeft;

    @Schema(description = "Timestamp when 2FA was first enabled - DEPRECATED", example = "2026-03-20T15:30:00")
    private LocalDateTime enabledAt;

    @ArraySchema(schema = @Schema(description = "List of backup codes with their usage status - DEPRECATED"))
    private List<BackupCodeStatusDto> backupCodesList;

    @Schema(description = "URL to download backup codes as text file", example = "/api/v1/auth/2fa/backup-codes/download")
    private String downloadBackupCodesUrl;

    // --- NEW FIELDS FOR MULTIPLE 2FA METHODS SUPPORT ---

    @Schema(description = "TOTP authenticator enabled status", example = "true")
    private boolean totpEnabled;

    @Schema(description = "Email OTP enabled status", example = "true")
    private boolean emailOtpEnabled;

    @Schema(description = "Backup codes available status", example = "true")
    private boolean backupCodesEnabled;

    @ArraySchema(schema = @Schema(description = "Detailed status of each enabled 2FA method"))
    private List<TwoFactorMethodStatusResponse> methodStatuses;

    @Schema(description = "List of all 2FA method types available", example = "[\"TOTP\", \"EMAIL\"]")
    private List<String> enabledMethods;

    @Schema(description = "Number of 2FA methods currently enabled", example = "2")
    private int enabledMethodCount;
}


