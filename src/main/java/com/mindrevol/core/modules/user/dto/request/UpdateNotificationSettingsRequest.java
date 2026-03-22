package com.mindrevol.core.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class UpdateNotificationSettingsRequest {
    @JsonAlias({"comment", "comments", "commentEnabled"})
    private Boolean commentEnabled;

    @JsonAlias({"message", "messages", "messageEnabled"})
    private Boolean messageEnabled;

    @JsonAlias({"journey", "journeys", "journeyEnabled"})
    private Boolean journeyEnabled;

    @JsonAlias({"friend", "friends", "friendRequest", "friendRequestEnabled"})
    private Boolean friendRequestEnabled;

    @JsonAlias({"reaction", "reactions", "reactionEnabled"})
    private Boolean reactionEnabled;

    @JsonAlias({"mention", "mentions", "mentionEnabled"})
    private Boolean mentionEnabled;

    @JsonAlias({"boxInvite", "boxInvitation", "boxInviteEnabled"})
    private Boolean boxInviteEnabled;

    private Boolean emailDailyReminder;
    private Boolean emailUpdates;
    private Boolean pushFriendRequest;
    private Boolean pushNewComment;
    private Boolean pushJourneyInvite;
    private Boolean pushReaction;
    private Boolean pushMessage;
    private Boolean pushMention;
    private Boolean pushBoxInvite;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;

    private Boolean inAppComment;
    private Boolean inAppReaction;
    private Boolean inAppMessage;
    private Boolean inAppJourney;
    private Boolean inAppFriendRequest;
    private Boolean inAppBoxInvite;
    private Boolean inAppMention;

    private Boolean pushComment;
    private Boolean pushJourney;
    private Boolean pushFriendRequestCategory;

    private Boolean emailComment;
    private Boolean emailReaction;
    private Boolean emailMessage;
    private Boolean emailJourney;
    private Boolean emailFriendRequest;
    private Boolean emailBoxInvite;
    private Boolean emailMention;

    // BỔ SUNG SPRINT 2 (TASK-202): Chế độ DND
    private Boolean dndEnabled;
    private Integer dndStartHour;
    private Integer dndEndHour;
}