package com.mindrevol.core.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private boolean pushEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    // BỔ SUNG SPRINT 2 (TASK-202): Chế độ Không làm phiền (DND)
    @Column(name = "dnd_enabled", columnDefinition = "boolean default false")
    private Boolean dndEnabled = false;

    @Column(name = "dnd_start_hour", columnDefinition = "int default 22")
    private Integer dndStartHour = 22;

    @Column(name = "dnd_end_hour", columnDefinition = "int default 6")
    private Integer dndEndHour = 6;
}