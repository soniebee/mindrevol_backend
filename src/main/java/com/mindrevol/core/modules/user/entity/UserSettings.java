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

    @Column(name = "push_message")
    @Builder.Default
    private boolean pushMessage = true;

    @Column(name = "push_mention")
    @Builder.Default
    private boolean pushMention = true;

    @Column(name = "push_box_invite")
    @Builder.Default
    private boolean pushBoxInvite = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private boolean pushEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    // [TASK-201] Tuỳ chỉnh nhận In-app theo danh mục
    @Column(name = "in_app_comment")
    @Builder.Default
    private boolean inAppComment = true;

    @Column(name = "in_app_reaction")
    @Builder.Default
    private boolean inAppReaction = true;

    @Column(name = "in_app_message")
    @Builder.Default
    private boolean inAppMessage = true;

    @Column(name = "in_app_journey")
    @Builder.Default
    private boolean inAppJourney = true;

    @Column(name = "in_app_friend_request")
    @Builder.Default
    private boolean inAppFriendRequest = true;

    @Column(name = "in_app_box_invite")
    @Builder.Default
    private boolean inAppBoxInvite = true;

    @Column(name = "in_app_mention")
    @Builder.Default
    private boolean inAppMention = true;

    // [TASK-201] Tuỳ chỉnh nhận Push theo danh mục
    @Column(name = "push_comment")
    @Builder.Default
    private boolean pushComment = true;

    @Column(name = "push_journey")
    @Builder.Default
    private boolean pushJourney = true;

    @Column(name = "push_friend_request_category")
    @Builder.Default
    private boolean pushFriendRequestCategory = true;

    // [TASK-201] Tuỳ chỉnh nhận Email theo danh mục
    @Column(name = "email_comment")
    @Builder.Default
    private boolean emailComment = false;

    @Column(name = "email_reaction")
    @Builder.Default
    private boolean emailReaction = false;

    @Column(name = "email_message")
    @Builder.Default
    private boolean emailMessage = false;

    @Column(name = "email_journey")
    @Builder.Default
    private boolean emailJourney = true;

    @Column(name = "email_friend_request")
    @Builder.Default
    private boolean emailFriendRequest = true;

    @Column(name = "email_box_invite")
    @Builder.Default
    private boolean emailBoxInvite = true;

    @Column(name = "email_mention")
    @Builder.Default
    private boolean emailMention = false;

    // BỔ SUNG SPRINT 2 (TASK-202): Chế độ Không làm phiền (DND)
    @Column(name = "dnd_enabled", columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean dndEnabled = false;

    @Column(name = "dnd_start_hour", columnDefinition = "int default 22")
    @Builder.Default
    private Integer dndStartHour = 22;

    @Column(name = "dnd_end_hour", columnDefinition = "int default 6")
    @Builder.Default
    private Integer dndEndHour = 6;
}