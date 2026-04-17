package com.mindrevol.core.modules.notification.service;

import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.notification.dto.PushNotificationTask;
import com.mindrevol.core.modules.notification.dto.WebSocketNotificationTask;
import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.entity.Notification;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserSettings;
import com.mindrevol.core.modules.user.repository.UserSettingsRepository;
import com.mindrevol.core.modules.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final AsyncTaskProducer asyncTaskProducer;
    private final UserSettingsRepository userSettingsRepository;
    private final UserPresenceService userPresenceService;

    public void dispatchPush(User recipient, User sender, Notification notification) {
        if (recipient.getFcmToken() == null || recipient.getFcmToken().isBlank()) {
            return;
        }

        if ((notification.getType() == NotificationType.DM_NEW_MESSAGE || notification.getType() == NotificationType.BOXCHAT_NEW_MESSAGE)
                && userPresenceService.isUserOnline(recipient.getId())) {
            return;
        }

        UserSettings settings = userSettingsRepository.findByUserId(recipient.getId()).orElse(null);
        if (settings != null) {
            if (!isCategoryTypeAllowed(settings, notification.getType())) {
                return;
            }
            if (isDndActive(settings, recipient.getTimezone())) {
                return;
            }
        }

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", notification.getType().name());

        if (notification.getReferenceId() != null) dataPayload.put("referenceId", notification.getReferenceId());
        if (sender != null) dataPayload.put("senderId", sender.getId());
        if (notification.getImageUrl() != null) dataPayload.put("imageUrl", notification.getImageUrl());
        if (notification.getActionStatus() != null) dataPayload.put("actionStatus", notification.getActionStatus());
        if (notification.getMessageKey() != null) dataPayload.put("messageKey", notification.getMessageKey());
        if (notification.getMessageArgs() != null) dataPayload.put("messageArgs", notification.getMessageArgs());

        asyncTaskProducer.submitPushNotificationTask(PushNotificationTask.builder()
                .recipientId(recipient.getId())
                .fcmToken(recipient.getFcmToken())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .dataPayload(dataPayload)
                .retryCount(0)
                .build());
    }

    public void dispatchWebSocket(String recipientId, NotificationResponse response) {
        UserSettings settings = userSettingsRepository.findByUserId(recipientId).orElse(null);
        if (settings != null && !isCategoryTypeAllowed(settings, response.getType())) {
            return;
        }

        asyncTaskProducer.submitWebSocketNotificationTask(WebSocketNotificationTask.builder()
                .recipientId(recipientId)
                .response(response)
                .retryCount(0)
                .build());
    }

    // --- BỔ SUNG CHO MODULE PAYMENT ---
    public void dispatchPaymentSuccessWebSocket(String recipientId, String packageInfo) {
        try {
            // Giả lập một response để gửi thẳng xuống WebSocket cho Frontend bắt hiệu ứng
            NotificationResponse response = NotificationResponse.builder()
                    .id(UUID.randomUUID().toString())
                    // Yêu cầu: Bạn cần thêm PAYMENT_SUCCESS vào enum NotificationType
                    .type("PAYMENT_SUCCESS")
                    .title("Thanh toán thành công")
                    .message("Tài khoản đã được nâng cấp lên gói " + packageInfo)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            asyncTaskProducer.submitWebSocketNotificationTask(WebSocketNotificationTask.builder()
                    .recipientId(recipientId)
                    .response(response)
                    .retryCount(0)
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Vui lòng thêm PAYMENT_SUCCESS vào NotificationType enum. Lỗi: {}", e.getMessage());
        }
    }

    private boolean isDndActive(UserSettings settings, String userTimezone) {
        if (settings.getDndEnabled() == null || !settings.getDndEnabled()) return false;

        String tz = (userTimezone != null && !userTimezone.isBlank()) ? userTimezone : "Asia/Ho_Chi_Minh";
        int currentHour;
        try {
            currentHour = LocalTime.now(ZoneId.of(tz)).getHour();
        } catch (Exception ignored) {
            currentHour = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).getHour();
        }

        int start = settings.getDndStartHour() != null ? settings.getDndStartHour() : 22;
        int end = settings.getDndEndHour() != null ? settings.getDndEndHour() : 6;

        if (start <= end) {
            return currentHour >= start && currentHour < end;
        } else {
            return currentHour >= start || currentHour < end;
        }
    }
    
    private boolean isCategoryTypeAllowed(UserSettings settings, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        try {
            NotificationType notificationType = NotificationType.valueOf(type);
            return isCategoryTypeAllowed(settings, notificationType);
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    private boolean isCategoryTypeAllowed(UserSettings settings, NotificationType type) {
        return switch (type) {
            case COMMENT, MOOD_COMMENT_RECEIVED -> settings.isInAppComment() || settings.isPushComment() || settings.isPushNewComment() || settings.isEmailComment();
            case REACTION -> settings.isInAppReaction() || settings.isPushReaction() || settings.isEmailReaction();
            case FRIEND_REQUEST -> settings.isInAppFriendRequest() || settings.isPushFriendRequestCategory() || settings.isPushFriendRequest() || settings.isEmailFriendRequest();
            case BOX_INVITE -> settings.isInAppBoxInvite() || settings.isPushBoxInvite() || settings.isEmailBoxInvite();
            case JOURNEY_INVITE -> settings.isInAppJourney() || settings.isPushJourney() || settings.isPushJourneyInvite() || settings.isEmailJourney();
            case COMMENT_MENTIONED, MOOD_MENTIONED -> settings.isInAppMention() || settings.isPushMention() || settings.isEmailMention();
            case DM_NEW_MESSAGE, BOXCHAT_NEW_MESSAGE -> settings.isInAppMessage() || settings.isPushMessage() || settings.isEmailMessage();
            default -> true;
        };
    }
}