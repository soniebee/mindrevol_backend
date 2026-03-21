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
import com.mindrevol.core.modules.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
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
	private final UserPresenceService userPresenceService;

	@Async
	@Transactional
	public void sendAndSaveNotification(String recipientId, String senderId, NotificationType type,
										String title, String message, String referenceId, String imageUrl) {
		sendAndSaveNotificationFull(
				recipientId,
				senderId,
				type,
				title,
				message,
				referenceId,
				imageUrl,
				null,
				null,
				null
		);
	}

	@Async
	@Transactional
	public void sendAndSaveNotificationFull(String recipientId, String senderId, NotificationType type,
										String title, String message, String referenceId, String imageUrl,
										String messageKey, String messageArgs, String actionStatus) {

		if (shouldThrottle(recipientId, senderId, type, referenceId)) {
			log.debug("Spam protection: skipped notification recipient={} sender={} type={} ref={}",
					recipientId, senderId, type, referenceId);
			return;
		}

		User recipient = userRepository.findById(recipientId)
				.orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

		UserSettings settings = userSettingsRepository.findByUserId(recipientId)
				.orElseGet(() -> UserSettings.builder().user(recipient).build());

		boolean isRealtimeAllowed = settings.isInAppEnabled();
		boolean isPushAllowed = checkPushSettings(type, settings);

		User sender = null;
		if (senderId != null) {
			sender = userRepository.findById(senderId).orElse(null);
		}

		Notification notification = createOrAggregateNotification(
				recipient,
				sender,
				type,
				title,
				message,
				referenceId,
				imageUrl,
				messageKey,
				messageArgs,
				actionStatus
		);
		notificationRepository.save(notification);

		NotificationResponse response = toResponse(notification);

		if (isRealtimeAllowed) {
			notificationDispatchService.dispatchWebSocket(recipient.getId(), response);
		}

		if (isPushAllowed && !shouldDebounceDirectPush(recipientId, senderId, type)) {
			notificationDispatchService.dispatchPush(recipient, sender, notification);
		}
	}

	private Notification createOrAggregateNotification(User recipient,
											 User sender,
											 NotificationType type,
											 String title,
											 String message,
											 String referenceId,
											 String imageUrl,
											 String messageKey,
											 String messageArgs,
											 String actionStatus) {
		if (type == NotificationType.REACTION && referenceId != null) {
			return notificationRepository
					.findFirstByRecipientIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
							recipient.getId(), type, referenceId)
					.map(existing -> {
						boolean isNewActor = sender != null
								&& (existing.getSender() == null || !sender.getId().equals(existing.getSender().getId()));
						if (isNewActor) {
							existing.setActorsCount(existing.getActorsCount() + 1);
						}
						existing.setSender(sender);
						existing.setTitle(title);
						existing.setMessage(message);
						existing.setImageUrl(imageUrl);
						existing.setMessageKey(messageKey);
						existing.setMessageArgs(messageArgs);
						existing.setActionStatus(actionStatus);
						existing.setRead(false);
						existing.setSeen(false);
						return existing;
					})
					.orElseGet(() -> buildNotification(
							recipient,
							sender,
							type,
							title,
							message,
							referenceId,
							imageUrl,
							messageKey,
							messageArgs,
							actionStatus
					));
		}

		return buildNotification(recipient, sender, type, title, message, referenceId, imageUrl, messageKey, messageArgs, actionStatus);
	}

	private Notification buildNotification(User recipient,
										  User sender,
										  NotificationType type,
										  String title,
										  String message,
										  String referenceId,
										  String imageUrl,
										  String messageKey,
										  String messageArgs,
										  String actionStatus) {
		return Notification.builder()
				.recipient(recipient)
				.sender(sender)
				.type(type)
				.title(title)
				.message(message)
				.referenceId(referenceId)
				.imageUrl(imageUrl)
				.messageKey(messageKey)
				.messageArgs(messageArgs)
				.actionStatus(actionStatus)
				.isRead(false)
				.isSeen(false)
				.actorsCount(1)
				.build();
	}

	private boolean checkPushSettings(NotificationType type, UserSettings settings) {
		if (type == null) {
			return settings.isPushEnabled();
		}
		if (!settings.isPushEnabled() || isInDnd(settings)) {
			return false;
		}

		return switch (type) {
			case FRIEND_REQUEST -> settings.isPushFriendRequest();
			case COMMENT -> settings.isPushNewComment();
			case REACTION -> settings.isPushReaction();
			case JOURNEY_INVITE -> settings.isPushJourneyInvite();
			default -> true;
		};
	}

	private boolean shouldThrottle(String recipientId, String senderId, NotificationType type, String referenceId) {
		if (type != NotificationType.REACTION && type != NotificationType.COMMENT) {
			return false;
		}
		String senderPart = senderId != null ? senderId : "system";
		String refPart = referenceId != null ? referenceId : "no-ref";
		String key = "noti_throttle:" + recipientId + ":" + senderPart + ":" + type + ":" + refPart;

		if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			return true;
		}
		redisTemplate.opsForValue().set(key, "1", 10, TimeUnit.SECONDS);
		return false;
	}

	private boolean shouldDebounceDirectPush(String recipientId, String senderId, NotificationType type) {
		if (type != NotificationType.DM_NEW_MESSAGE && type != NotificationType.BOXCHAT_NEW_MESSAGE) {
			return false;
		}
		if (userPresenceService.isUserOnline(recipientId)) {
			return true;
		}
		if (senderId == null) {
			return false;
		}

		String key = "noti_dm_push_debounce:" + recipientId + ":" + senderId + ":" + type;
		if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			return true;
		}
		redisTemplate.opsForValue().set(key, "1", 1, TimeUnit.MINUTES);
		return false;
	}

	private boolean isInDnd(UserSettings settings) {
		if (!Boolean.TRUE.equals(settings.getDndEnabled())) {
			return false;
		}

		Integer start = settings.getDndStartHour();
		Integer end = settings.getDndEndHour();
		if (start == null || end == null || start < 0 || start > 23 || end < 0 || end > 23) {
			return false;
		}

		int now = LocalTime.now().getHour();
		if (start.equals(end)) {
			return true;
		}
		if (start < end) {
			return now >= start && now < end;
		}
		return now >= start || now < end;
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getMyNotifications(String userId, Pageable pageable) {
		return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
				.map(this::toResponse);
	}

	@Transactional
	public void markAsRead(String notificationId, String userId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
		if (!notification.getRecipient().getId().equals(userId)) return;
		notification.setRead(true);
		notification.setSeen(true);
		notificationRepository.save(notification);
	}

	@Transactional
	public void markAllAsRead(String userId) {
		notificationRepository.markAllAsRead(userId);
	}

	@Transactional
	public void markAllAsSeen(String userId) {
		notificationRepository.markAllAsSeen(userId);
	}

	@Transactional(readOnly = true)
	public long countUnseen(String userId) {
		return notificationRepository.countByRecipientIdAndIsSeenFalse(userId);
	}

	@Transactional(readOnly = true)
	public long countUnread(String userId) {
		return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
	}

	@Transactional
	public void updateActionStatusForNotification(String recipientId,
												NotificationType type,
												String referenceId,
												String actionStatus) {
		notificationRepository.updateActionStatusByRecipientAndTypeAndReferenceId(
				recipientId,
				type,
				referenceId,
				actionStatus
		);
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
		String acceptUrl = null;
		String rejectUrl = null;
		if (n.getType() == NotificationType.FRIEND_REQUEST && n.getReferenceId() != null) {
			acceptUrl = "/api/v1/friends/accept/" + n.getReferenceId();
			rejectUrl = "/api/v1/friends/decline/" + n.getReferenceId();
		} else if (n.getType() == NotificationType.BOX_INVITE && n.getReferenceId() != null) {
			acceptUrl = "/api/v1/boxes/invitations/" + n.getReferenceId() + "?accept=true";
			rejectUrl = "/api/v1/boxes/invitations/" + n.getReferenceId() + "?accept=false";
		}

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
				.messageKey(n.getMessageKey())
				.messageArgs(n.getMessageArgs())
				.actionStatus(n.getActionStatus())
				.actionAcceptUrl(acceptUrl)
				.actionRejectUrl(rejectUrl)
				.createdAt(n.getCreatedAt())
				.senderId(n.getSender() != null ? n.getSender().getId() : null)
				.senderName(n.getSender() != null ? n.getSender().getFullname() : "Hệ thống")
				.build();
	}
}