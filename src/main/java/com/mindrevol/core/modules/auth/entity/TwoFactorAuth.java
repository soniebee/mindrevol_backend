package com.mindrevol.core.modules.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to store multiple 2FA methods per user.
 * This allows users to have multiple 2FA methods enabled simultaneously.
 * For example: TOTP + EMAIL OTP + Backup Codes
 */
@Entity
@Table(name = "two_factor_auth", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "method"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorAuth extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "method", nullable = false, length = 50)
    private String method; // "TOTP", "EMAIL", etc.

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "secret", length = 128)
    private String secret; // TOTP secret

    @Column(name = "temp_secret", length = 128)
    private String tempSecret; // Temporary secret for setup

    @Column(name = "email", length = 255)
    private String email; // Email for EMAIL OTP method

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes; // Serialized list of backup codes

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Check if this 2FA method is properly configured and ready to use
     */
    public boolean isReadyToUse() {
        if (!enabled) {
            return false;
        }
        
        return switch (method.toUpperCase()) {
            case "TOTP" -> secret != null && !secret.isBlank();
            case "EMAIL" -> email != null && !email.isBlank() && Boolean.TRUE.equals(emailVerified);
            case "BACKUP_CODES" -> backupCodes != null && !backupCodes.isBlank();
            default -> false;
        };
    }
}
