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
}