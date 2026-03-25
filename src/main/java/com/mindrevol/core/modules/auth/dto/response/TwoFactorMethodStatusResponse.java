package com.mindrevol.core.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response containing information about a single 2FA method
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class TwoFactorMethodStatusResponse {

    @Schema(description = "Method type", example = "TOTP")
    private String method;

    @Schema(description = "Whether this method is enabled", example = "true")
    private boolean enabled;

    @Schema(description = "Whether this method is properly configured and ready to use", example = "true")
    private boolean readyToUse;

    @Schema(description = "Email address if method is EMAIL (masked)", example = "user***@example.com")
    private String email;

    @Schema(description = "Number of backup codes remaining (for BACKUP_CODES method)", example = "5")
    private int backupCodesRemaining;

    @Schema(description = "Timestamp when this method was enabled", example = "2026-03-20T15:30:00")
    private LocalDateTime enabledAt;

    @Schema(description = "Timestamp when this method was last used", example = "2026-03-23T10:15:00")
    private LocalDateTime lastUsedAt;

    @Schema(description = "Whether email verification is pending (for EMAIL method)", example = "false")
    private boolean emailVerificationPending;
}
