package com.mindrevol.core.modules.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class FirebaseService {

    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public FirebaseService(Optional<FirebaseMessaging> firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging.orElse(null);
    }

    public boolean sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.debug("Firebase is disabled. Skipping FCM notification to token: {}", fcmToken);
            return false;
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            return false;
        }

        try {
            // Xây dựng message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Thêm data payload (để Client xử lý điều hướng khi bấm vào)
            if (data != null) {
                messageBuilder.putAllData(data);
            }

            // Gửi
            String response = firebaseMessaging.send(messageBuilder.build());
            log.debug("Sent FCM message: {}", response);
            return true;

        } catch (Exception e) {
            log.warn("Failed to send FCM message to token {}: {}", fcmToken, e.getMessage());
            throw new IllegalStateException("FCM send failed", e);
        }
    }
}
