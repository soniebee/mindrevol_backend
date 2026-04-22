package com.mindrevol.core.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationTask implements Serializable {
    private String recipientId;
    private String fcmToken;
    private String title;
    private String message;
    private Map<String, String> dataPayload;
    private int retryCount;
}

