package com.mindrevol.core.modules.user.dto.request;

import lombok.Data;

@Data
public class UpdateNotificationSettingsRequest {
    private Boolean emailDailyReminder;
    private Boolean emailUpdates;
    private Boolean pushFriendRequest;
    private Boolean pushNewComment;
    private Boolean pushJourneyInvite;
    private Boolean pushReaction;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;

    // BỔ SUNG SPRINT 2 (TASK-202): Chế độ DND
    private Boolean dndEnabled;
    private Integer dndStartHour;
    private Integer dndEndHour;
}