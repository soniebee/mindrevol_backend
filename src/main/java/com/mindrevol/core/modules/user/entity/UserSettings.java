package com.mindrevol.core.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "email_daily_reminder")
    @Builder.Default
    private boolean emailDailyReminder = true;

    @Column(name = "email_updates")
    @Builder.Default
    private boolean emailUpdates = true;

    @Column(name = "push_friend_request")
    @Builder.Default
    private boolean pushFriendRequest = true;

    @Column(name = "push_new_comment")
    @Builder.Default
    private boolean pushNewComment = true;

    @Column(name = "push_journey_invite")
    @Builder.Default
    private boolean pushJourneyInvite = true;

    @Column(name = "push_reaction")
    @Builder.Default
    private boolean pushReaction = true;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 128)
    private String twoFactorSecret;

    @Column(name = "two_factor_temp_secret", length = 128)
    private String twoFactorTempSecret;

    @Column(name = "two_factor_backup_codes", columnDefinition = "TEXT")
    private String twoFactorBackupCodes;

    @Column(name = "two_factor_enabled_at")
    private LocalDateTime twoFactorEnabledAt;

    // --- NEW FIELDS FOR ENHANCED 2FA ---

    @Column(name = "two_factor_method", length = 50)
    private String twoFactorMethod; // "TOTP" or "EMAIL"

    @Column(name = "two_factor_email", length = 255)
    private String twoFactorEmail; // Email for 2FA code delivery

    @Column(name = "two_factor_email_verification_token", length = 255)
    private String twoFactorEmailVerificationToken;

    @Column(name = "two_factor_email_verified")
    @Builder.Default
    private Boolean twoFactorEmailVerified = false;
}