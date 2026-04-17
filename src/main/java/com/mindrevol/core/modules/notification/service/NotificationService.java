package com.mindrevol.core.modules.notification.service;

import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.entity.Notification;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.repository.NotificationRepository;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserSettings;
import com.mindrevol.core.modules.user.repository.UserRepository;
import com.mindrevol.core.modules.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final UserSettingsRepository userSettingsRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final NotificationDispatchService notificationDispatchService;

	@Async
	@Transactional
	public void sendAndSaveNotification(String recipientId, String senderId, NotificationType type,
										String title, String message, String referenceId, String imageUrl) {
		sendAndSaveNotificationFull(recipientId, senderId, type, title, message, referenceId, imageUrl, null, null, null);
	}

	@Async
	@Transactional
	public void sendAndSaveNotificationFull(String recipientId, String senderId, NotificationType type,
											String title, String message, String referenceId, String imageUrl,
											String messageKey, String messageArgs, String actionStatus) {

		User recipient = userRepository.findById(recipientId)
				.orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

		UserSettings settings = userSettingsRepository.findByUserId(recipientId).orElse(null);
		if (settings != null && !isCategoryEnabled(settings, type)) {
			return;
		}

		if (type == NotificationType.DM_NEW_MESSAGE || type == NotificationType.BOXCHAT_NEW_MESSAGE) {
			String safeSender = senderId == null ? "system" : senderId;
			String safeReference = referenceId == null ? "none" : referenceId;
			String debounceKey = "noti_chat_debounce:" + recipientId + ":" + safeSender + ":" + type + ":" + safeReference;

			Boolean firstInWindow = redisTemplate.opsForValue().setIfAbsent(debounceKey, "1", 60, TimeUnit.SECONDS);
			if (!Boolean.TRUE.equals(firstInWindow)) {
				return;
			}
		}

		if (type == NotificationType.REACTION || type == NotificationType.COMMENT) {
			String safeSender = senderId == null ? "system" : senderId;
			String safeReference = referenceId == null ? "none" : referenceId;
			String throttleKey = "noti_throttle:" + recipientId + ":" + safeSender + ":" + type + ":" + safeReference;

			if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey))) {
				return;
			}
			redisTemplate.opsForValue().set(throttleKey, "1", 30, TimeUnit.SECONDS);
		}


		User sender = null;
		if (senderId != null) {
			sender = userRepository.findById(senderId).orElse(null);
		}

		if (tryAggregateReactionNotification(recipient, sender, type, referenceId, imageUrl, message, messageKey, messageArgs, actionStatus)) {
			return;
		}

		Notification notification = Notification.builder()
				.recipient(recipient)
				.sender(sender)
				.type(type)
				.title(title)
				.message(message)
				.referenceId(referenceId)
				.imageUrl(imageUrl)
				.isRead(false)
				.isSeen(false)
				.actorsCount(1)
				.actionStatus(actionStatus)
				.messageKey(messageKey)
				.messageArgs(messageArgs)
				.build();

		notificationRepository.save(notification);
		pushToFirebaseAndWebSocket(recipient, sender, notification);
	}

	private boolean tryAggregateReactionNotification(User recipient, User sender, NotificationType type,
										 String referenceId, String imageUrl, String fallbackMessage,
										 String messageKey, String messageArgs, String actionStatus) {
		if (type != NotificationType.REACTION || referenceId == null || sender == null) {
			return false;
		}

		Optional<Notification> existingOpt = notificationRepository
				.findFirstByRecipientIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(recipient.getId(), type, referenceId);

		if (existingOpt.isEmpty()) {
			return false;
		}

		Notification existing = existingOpt.get();
		String previousSenderId = existing.getSender() != null ? existing.getSender().getId() : null;
		if (sender.getId().equals(previousSenderId)) {
			// Cùng 1 người thả cảm xúc liên tục -> không tạo thêm thông báo mới
			return true;
		}

		String previousSenderName = existing.getSender() != null ? existing.getSender().getFullname() : null;
		int updatedActorsCount = Math.max(existing.getActorsCount(), 1) + 1;

		existing.setActorsCount(updatedActorsCount);
		existing.setSender(sender);
		existing.setImageUrl(imageUrl);
		existing.setMessageKey(messageKey);
		existing.setMessageArgs(messageArgs);
		existing.setActionStatus(actionStatus);
		existing.setSeen(false);
		existing.setMessage(buildAggregatedReactionMessage(sender.getFullname(), previousSenderName, updatedActorsCount, fallbackMessage));

		notificationRepository.save(existing);
		pushToFirebaseAndWebSocket(recipient, sender, existing);
		return true;
	}

	private String buildAggregatedReactionMessage(String latestActorName, String previousActorName, int actorsCount, String fallbackMessage) {
		if (latestActorName == null || latestActorName.isBlank()) {
			return fallbackMessage;
		}

		if (actorsCount <= 1 || previousActorName == null || previousActorName.isBlank()) {
			return latestActorName + " đã bày tỏ cảm xúc về bài viết của bạn.";
		}

		if (actorsCount == 2) {
			return latestActorName + " và " + previousActorName + " đã bày tỏ cảm xúc về bài viết của bạn.";
		}

		return latestActorName + ", " + previousActorName + " và " + (actorsCount - 2)
				+ " người khác đã bày tỏ cảm xúc về bài viết của bạn.";
	}

	private void pushToFirebaseAndWebSocket(User recipient, User sender, Notification notification) {
		notificationDispatchService.dispatchPush(recipient, sender, notification);
		notificationDispatchService.dispatchWebSocket(recipient.getId(), toResponse(notification));
	}

	@Transactional
	public void updateActionStatusForNotification(String recipientId, NotificationType type, String referenceId, String actionStatus) {
		if (referenceId == null || actionStatus == null || actionStatus.isBlank()) {
			return;
		}

		notificationRepository
				.findFirstByRecipientIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(recipientId, type, referenceId)
				.ifPresent(notification -> {
					notification.setActionStatus(actionStatus);
					notification.setSeen(false);
					notificationRepository.save(notification);
				});
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getMyNotifications(String userId, Pageable pageable) {
		// Giữ lịch sử thông báo đã phát sinh trước khi user tắt category.
		return notificationRepository.findByRecipientIdOrderByUpdatedAtDescCreatedAtDesc(userId, pageable)
				.map(this::toResponse);
	}

	@Transactional
	public void markAsRead(String notificationId, String userId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

		if (!notification.getRecipient().getId().equals(userId)) {
			return;
		}

		notification.setRead(true);
		notification.setSeen(true); // Read thì chắc chắn là Seen rồi
		notificationRepository.save(notification);
	}

	@Transactional
	public void markAllAsRead(String userId) {
		notificationRepository.markAllAsRead(userId);
	}

	// [TASK-102] Đổi đếm unread thành unseen cho icon chuông
	@Transactional(readOnly = true)
	public long countUnseen(String userId) {
		// Badge unseen phản ánh toàn bộ thông báo đang có trong DB.
		return notificationRepository.countByRecipientIdAndIsSeenFalse(userId);
	}

	// [TASK-102] Đánh dấu đã thấy (Seen) khi mở menu
	@Transactional
	public void markAllAsSeen(String userId) {
		notificationRepository.markAllAsSeen(userId);
	}

	@Transactional
	public void deleteNotification(String notificationId, String userId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));

		if (notification.getRecipient().getId().equals(userId)) {
			notificationRepository.delete(notification);
		}
	}

	@Transactional
	public void deleteAllMyNotifications(String userId) {
		notificationRepository.deleteAllByRecipientId(userId);
	}

	private NotificationResponse toResponse(Notification n) {
		return NotificationResponse.builder()
				.id(n.getId())
				.title(n.getTitle())
				.message(n.getMessage())
				.type(n.getType() != null ? n.getType().name() : null)
				.referenceId(n.getReferenceId())
				.imageUrl(n.getImageUrl())
				.isRead(n.isRead())
				.isSeen(n.isSeen())
				.actorsCount(n.getActorsCount())
				.actionStatus(n.getActionStatus())
				.messageKey(n.getMessageKey())
				.messageArgs(n.getMessageArgs())
				.actionUrls(buildActionUrls(n))
				.createdAt(n.getCreatedAt())
				.senderId(n.getSender() != null ? n.getSender().getId() : null)
				.senderName(n.getSender() != null ? n.getSender().getFullname() : "Hệ thống")
				.build();
	}

	private Map<String, String> buildActionUrls(Notification notification) {
		if (notification.getReferenceId() == null) {
			return Collections.emptyMap();
		}

		return switch (notification.getType()) {
			case FRIEND_REQUEST -> Map.of(
					"accept", "/api/v1/friends/accept/" + notification.getReferenceId(),
					"reject", "/api/v1/friends/decline/" + notification.getReferenceId()
			);
			case BOX_INVITE -> Map.of(
					"accept", "/api/v1/boxes/invitations/" + notification.getReferenceId() + "?accept=true",
					"reject", "/api/v1/boxes/invitations/" + notification.getReferenceId() + "?accept=false"
			);
			case JOURNEY_INVITE -> Map.of(
					"accept", "/api/v1/journey-invitations/" + notification.getReferenceId() + "/accept",
					"reject", "/api/v1/journey-invitations/" + notification.getReferenceId() + "/reject"
			);
			default -> Collections.emptyMap();
		};
	}

	private boolean isCategoryEnabled(UserSettings settings, NotificationType type) {
		return switch (type) {
			case COMMENT, MOOD_COMMENT_RECEIVED -> settings.isInAppComment() || settings.isPushComment() || settings.isPushNewComment() || settings.isEmailComment();
			case REACTION -> settings.isInAppReaction() || settings.isPushReaction() || settings.isEmailReaction();
			case FRIEND_REQUEST, FRIEND_REQUEST_RECEIVED -> settings.isInAppFriendRequest() || settings.isPushFriendRequestCategory() || settings.isPushFriendRequest() || settings.isEmailFriendRequest();
			case BOX_INVITE, BOX_INVITE_RECEIVED -> settings.isInAppBoxInvite() || settings.isPushBoxInvite() || settings.isEmailBoxInvite();
			case JOURNEY_INVITE -> settings.isInAppJourney() || settings.isPushJourney() || settings.isPushJourneyInvite() || settings.isEmailJourney();
			case MOOD_MENTIONED -> settings.isInAppMention() || settings.isPushMention() || settings.isEmailMention();
			case DM_NEW_MESSAGE, BOXCHAT_NEW_MESSAGE -> settings.isInAppMessage() || settings.isPushMessage() || settings.isEmailMessage();
			default -> true;
		};
	}
}