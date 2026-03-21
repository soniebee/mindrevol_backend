package com.mindrevol.core.modules.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

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

    // --- BỔ SUNG SPRINT 2 (EPIC 1 & 3) ---
    private String messageKey;
    private String messageArgs;
    private String actionStatus;
    private String actionAcceptUrl;
    private String actionRejectUrl;
    // -------------------------------------

    private LocalDateTime createdAt;
    private String senderId;
    private String senderName;
}