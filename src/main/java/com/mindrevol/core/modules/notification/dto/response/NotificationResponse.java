package com.mindrevol.core.modules.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private String type;
    private String referenceId;
    private String imageUrl;

    @JsonProperty("isRead")
    private boolean isRead;

    @JsonProperty("isSeen")
    private boolean isSeen;

    private int actorsCount;
    private String actionStatus;
    private String messageKey;
    private String messageArgs;
    private Map<String, String> actionUrls;

    private LocalDateTime createdAt;
    private String senderId;
    private String senderName;
}