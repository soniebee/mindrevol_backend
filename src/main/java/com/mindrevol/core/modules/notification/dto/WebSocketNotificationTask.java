package com.mindrevol.core.modules.notification.dto;

import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketNotificationTask implements Serializable {
    private String recipientId;
    private NotificationResponse response;
    private int retryCount;
}

