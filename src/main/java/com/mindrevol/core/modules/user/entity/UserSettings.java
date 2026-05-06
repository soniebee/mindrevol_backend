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
    private Boolean emailDailyReminder = true;

    @Column(name = "email_updates")
    @Builder.Default
    private Boolean emailUpdates = true;

    @Column(name = "push_friend_request")
    @Builder.Default
    private Boolean pushFriendRequest = true;

    @Column(name = "push_new_comment")
    @Builder.Default
    private Boolean pushNewComment = true;

    @Column(name = "push_journey_invite")
    @Builder.Default
    private Boolean pushJourneyInvite = true;

    @Column(name = "push_reaction")
    @Builder.Default
    private Boolean pushReaction = true;

    @Column(name = "push_message")
    @Builder.Default
    private Boolean pushMessage = true;

    @Column(name = "push_mention")
    @Builder.Default
    private Boolean pushMention = true;

    @Column(name = "push_box_invite")
    @Builder.Default
    private Boolean pushBoxInvite = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "in_app_comment")
    @Builder.Default
    private Boolean inAppComment = true;

    @Column(name = "in_app_reaction")
    @Builder.Default
    private Boolean inAppReaction = true;

    @Column(name = "in_app_message")
    @Builder.Default
    private Boolean inAppMessage = true;

    @Column(name = "in_app_journey")
    @Builder.Default
    private Boolean inAppJourney = true;

    @Column(name = "in_app_friend_request")
    @Builder.Default
    private Boolean inAppFriendRequest = true;

    @Column(name = "in_app_box_invite")
    @Builder.Default
    private Boolean inAppBoxInvite = true;

    @Column(name = "in_app_mention")
    @Builder.Default
    private Boolean inAppMention = true;

    @Column(name = "push_comment")
    @Builder.Default
    private Boolean pushComment = true;

    @Column(name = "push_journey")
    @Builder.Default
    private Boolean pushJourney = true;

    @Column(name = "push_friend_request_category")
    @Builder.Default
    private Boolean pushFriendRequestCategory = true;

    @Column(name = "email_comment")
    @Builder.Default
    private Boolean emailComment = false;

    @Column(name = "email_reaction")
    @Builder.Default
    private Boolean emailReaction = false;

    @Column(name = "email_message")
    @Builder.Default
    private Boolean emailMessage = false;

    @Column(name = "email_journey")
    @Builder.Default
    private Boolean emailJourney = true;

    @Column(name = "email_friend_request")
    @Builder.Default
    private Boolean emailFriendRequest = true;

    @Column(name = "email_box_invite")
    @Builder.Default
    private Boolean emailBoxInvite = true;

    @Column(name = "email_mention")
    @Builder.Default
    private Boolean emailMention = false;

    @Column(name = "dnd_enabled", columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean dndEnabled = false;

    @Column(name = "dnd_start_hour", columnDefinition = "int default 22")
    @Builder.Default
    private Integer dndStartHour = 22;

    @Column(name = "dnd_end_hour", columnDefinition = "int default 6")
    @Builder.Default
    private Integer dndEndHour = 6;

    // --- [THÊM MỚI] GHOST MODE CHO BẢN ĐỒ ---
    @Column(name = "location_visibility", length = 20)
    @Builder.Default
    private String locationVisibility = "PRECISE"; // PRECISE, BLURRED, HIDDEN
}