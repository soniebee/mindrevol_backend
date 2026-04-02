package com.mindrevol.core.modules.notification.entity;

public enum NotificationType {
    // Friend notifications
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,

    // Direct message notifications
    DM_NEW_MESSAGE,
    DM_SHARED_MOMENT,

    // Box chat notifications
    BOXCHAT_NEW_MESSAGE,
    BOXCHAT_SHARED_MOMENT,

    // Mood/Status notifications
    MOOD_NEW_STATUS_IN_BOX,
    MOOD_COMMENT_RECEIVED,

    // Mention notifications
    COMMENT_MENTIONED,
    MOOD_MENTIONED,

    // Moment/Journey notifications
    MOMENT_NEW_FROM_FRIEND,
    MOMENT_NEW_IN_BOX,
    MOMENT_REPLIED_IN_DM,
    MOMENT_ASSIGNED_TO_JOURNEY,
    JOURNEY_NEW_CREATED_IN_BOX,

    // Unsorted/Moment notifications
    UNSORTED_EXPIRING_SOON,
    UNSORTED_EXPIRED_DELETED,

    // Box member notifications
    BOX_INVITE,
    BOX_MEMBER_JOINED,
    BOX_MEMBER_REMOVED,
    BOX_ROLE_UPDATED,
    BOX_DISSOLVED,
    BOX_ADDED,

    // System notifications
    AUTH_NEW_LOGIN,
    PROFILE_UPDATED,
    UPLOAD_FAILED_RETRY,

    // Basic notifications
    REACTION,
    COMMENT,
    SYSTEM,
    JOURNEY_INVITE,
    CHECKIN
}
