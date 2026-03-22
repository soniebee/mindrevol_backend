package com.mindrevol.core.modules.notification.service;

import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.entity.Notification;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.repository.NotificationRepository;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final FirebaseService firebaseService;
	private final SimpMessagingTemplate messagingTemplate;

	@Async
	@Transactional
	public void sendAndSaveNotification(String recipientId, String senderId, NotificationType type,
										String title, String message, String referenceId, String imageUrl) {

		if (type == NotificationType.REACTION || type == NotificationType.COMMENT) {
			String throttleKey = "noti_throttle:" + recipientId + ":" + type + ":" + referenceId;

			if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey))) {
				log.info("Spam protection: Skipped notification for user {} type {} ref {}", recipientId, type, referenceId);
				return;
			}
			redisTemplate.opsForValue().set(throttleKey, "1", 30, TimeUnit.MINUTES);
		}

		User recipient = userRepository.findById(recipientId)
				.orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

		User sender = null;
		if (senderId != null) {
			sender = userRepository.findById(senderId).orElse(null);
		}

		// [TASK-101] LOGIC GOM NHÓM THÔNG BÁO (AGGREGATION)
		if (type == NotificationType.REACTION || type == NotificationType.COMMENT) {
			Optional<Notification> existingOpt = notificationRepository
					.findFirstByRecipientIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(recipientId, type, referenceId);

			if (existingOpt.isPresent()) {
				Notification existing = existingOpt.get();
				// Nếu người tương tác khác với người cuối cùng đã tương tác -> Gộp nhóm
				if (sender != null && !existing.getSender().getId().equals(senderId)) {
					existing.setActorsCount(existing.getActorsCount() + 1);
					existing.setSender(sender);
					existing.setImageUrl(imageUrl);

					String actionText = type == NotificationType.COMMENT ? "bình luận về" : "bày tỏ cảm xúc về";
					existing.setMessage(sender.getFullname() + " và " + (existing.getActorsCount() - 1) + " người khác đã " + actionText + " bài viết của bạn.");

					// Reset trạng thái để nổi lên lại
					existing.setSeen(false);
					existing.setCreatedAt(LocalDateTime.now());

					notificationRepository.save(existing);
					pushToFirebaseAndWebSocket(recipient, sender, existing);
					return;
				} else if (sender != null && existing.getSender().getId().equals(senderId)) {
					// Chống spam 1 người comment liên tục
					return;
				}
			}
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
				.build();

		notificationRepository.save(notification);
		pushToFirebaseAndWebSocket(recipient, sender, notification);
	}

	private void pushToFirebaseAndWebSocket(User recipient, User sender, Notification notification) {
		if (recipient.getFcmToken() != null) {
			Map<String, String> dataPayload = new HashMap<>();
			dataPayload.put("type", notification.getType().name());
			if (notification.getReferenceId() != null) dataPayload.put("referenceId", notification.getReferenceId());
			if (sender != null) dataPayload.put("senderId", sender.getId());
			if (notification.getImageUrl() != null) dataPayload.put("imageUrl", notification.getImageUrl());

			firebaseService.sendNotification(
					recipient.getFcmToken(),
					notification.getTitle(),
					notification.getMessage(),
					dataPayload
			);
		}

		NotificationResponse response = toResponse(notification);
		messagingTemplate.convertAndSendToUser(
				recipient.getId(),
				"/queue/notifications",
				response
		);
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
				.createdAt(n.getCreatedAt())
				.senderId(n.getSender() != null ? n.getSender().getId() : null)
				.senderName(n.getSender() != null ? n.getSender().getFullname() : "Hệ thống")
				.build();
	}
}