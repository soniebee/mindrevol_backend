package com.mindrevol.core.modules.notification.service;

import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.entity.Notification;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserSettings;
import com.mindrevol.core.modules.user.repository.UserSettingsRepository;
import com.mindrevol.core.modules.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;
    // BỔ SUNG: Tiêm repository để lấy cài đặt người dùng
    private final UserSettingsRepository userSettingsRepository;
    private final UserPresenceService userPresenceService;

    @Async("taskExecutor")
    public void dispatchPush(User recipient, User sender, Notification notification) {
        if (recipient.getFcmToken() == null || recipient.getFcmToken().isBlank()) {
            return;
        }

        // TASK-404: Nếu người nhận đang online thì không đẩy push chat để tránh spam.
        if ((notification.getType() == NotificationType.DM_NEW_MESSAGE || notification.getType() == NotificationType.BOXCHAT_NEW_MESSAGE)
                && userPresenceService.isUserOnline(recipient.getId())) {
            return;
        }

        // --- BẮT ĐẦU LOGIC TASK 201 & 202 ---
        UserSettings settings = userSettingsRepository.findByUserId(recipient.getId()).orElse(null);
        if (settings != null) {
            // 1. Kiểm tra master switch (Bật/tắt toàn bộ push)
            if (!settings.isPushEnabled()) {
                return;
            }

            // 2. Kiểm tra chế độ Không làm phiền (DND)
            if (isDndActive(settings, recipient.getTimezone())) {
                return;
            }

            // 3. Kiểm tra tuỳ chỉnh cho từng loại thông báo
            if (!isNotificationTypeAllowed(settings, notification.getType())) {
                return;
            }
        }
        // --- KẾT THÚC LOGIC TASK 201 & 202 ---

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", notification.getType().name());

        if (notification.getReferenceId() != null) dataPayload.put("referenceId", notification.getReferenceId());
        if (sender != null) dataPayload.put("senderId", sender.getId());
        if (notification.getImageUrl() != null) dataPayload.put("imageUrl", notification.getImageUrl());
        if (notification.getActionStatus() != null) dataPayload.put("actionStatus", notification.getActionStatus());
        if (notification.getMessageKey() != null) dataPayload.put("messageKey", notification.getMessageKey());
        if (notification.getMessageArgs() != null) dataPayload.put("messageArgs", notification.getMessageArgs());

        firebaseService.sendNotification(
                recipient.getFcmToken(),
                notification.getTitle(),
                notification.getMessage(),
                dataPayload
        );
    }

    @Async("taskExecutor")
    public void dispatchWebSocket(String recipientId, NotificationResponse response) {
        UserSettings settings = userSettingsRepository.findByUserId(recipientId).orElse(null);
        if (settings != null && (!settings.isInAppEnabled() || !isInAppTypeAllowed(settings, response.getType()))) {
            return;
        }

        messagingTemplate.convertAndSendToUser(recipientId, "/queue/notifications", response);
    }

    // --- HÀM HELPER CHO TASK 201 & 202 ---

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
            // Ví dụ DND từ 8h sáng đến 12h trưa
            return currentHour >= start && currentHour < end;
        } else {
            // Ví dụ DND qua đêm từ 22h đêm đến 6h sáng hôm sau
            return currentHour >= start || currentHour < end;
        }
    }

    private boolean isNotificationTypeAllowed(UserSettings settings, NotificationType type) {
        return switch (type) {
            case COMMENT, MOOD_COMMENT_RECEIVED -> settings.isPushComment() && settings.isPushNewComment();
            case REACTION -> settings.isPushReaction();
            case FRIEND_REQUEST, FRIEND_REQUEST_RECEIVED -> settings.isPushFriendRequestCategory() && settings.isPushFriendRequest();
            case BOX_INVITE, BOX_INVITE_RECEIVED -> settings.isPushBoxInvite();
            case JOURNEY_INVITE -> settings.isPushJourney() && settings.isPushJourneyInvite();
            case MOOD_MENTIONED -> settings.isPushMention();
            case DM_NEW_MESSAGE, BOXCHAT_NEW_MESSAGE -> settings.isPushMessage();
            default -> true;
        };
    }

    private boolean isInAppTypeAllowed(UserSettings settings, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }

        try {
            NotificationType notificationType = NotificationType.valueOf(type);
            return switch (notificationType) {
                case COMMENT, MOOD_COMMENT_RECEIVED -> settings.isInAppComment();
                case REACTION -> settings.isInAppReaction();
                case FRIEND_REQUEST, FRIEND_REQUEST_RECEIVED -> settings.isInAppFriendRequest();
                case BOX_INVITE, BOX_INVITE_RECEIVED -> settings.isInAppBoxInvite();
                case JOURNEY_INVITE -> settings.isInAppJourney();
                case MOOD_MENTIONED -> settings.isInAppMention();
                case DM_NEW_MESSAGE, BOXCHAT_NEW_MESSAGE -> settings.isInAppMessage();
                default -> true;
            };
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }
}