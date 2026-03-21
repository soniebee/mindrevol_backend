package com.mindrevol.core.modules.notification.service;

import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.entity.Notification;
import com.mindrevol.core.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("taskExecutor")
    public void dispatchPush(User recipient, User sender, Notification notification) {
        if (recipient.getFcmToken() == null || recipient.getFcmToken().isBlank()) {
            return;
        }

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", notification.getType().name());

        if (notification.getReferenceId() != null) {
            dataPayload.put("referenceId", notification.getReferenceId());
        }
        if (sender != null) {
            dataPayload.put("senderId", sender.getId());
        }
        if (notification.getImageUrl() != null) {
            dataPayload.put("imageUrl", notification.getImageUrl());
        }
        if (notification.getActionStatus() != null) {
            dataPayload.put("actionStatus", notification.getActionStatus());
        }
        if (notification.getMessageKey() != null) {
            dataPayload.put("messageKey", notification.getMessageKey());
        }
        if (notification.getMessageArgs() != null) {
            dataPayload.put("messageArgs", notification.getMessageArgs());
        }

        firebaseService.sendNotification(
                recipient.getFcmToken(),
                notification.getTitle(),
                notification.getMessage(),
                dataPayload
        );
    }

    @Async("taskExecutor")
    public void dispatchWebSocket(String recipientId, NotificationResponse response) {
        messagingTemplate.convertAndSendToUser(recipientId, "/queue/notifications", response);
    }
}

