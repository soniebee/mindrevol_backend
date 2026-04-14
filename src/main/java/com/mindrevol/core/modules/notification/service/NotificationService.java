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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final SimpMessagingTemplate messagingTemplate;

	// Make FirebaseService optional - it may not be available when firebase.enabled=false
	private final Optional<FirebaseService> firebaseService;

	public NotificationService(NotificationRepository notificationRepository,
							   UserRepository userRepository,
							   RedisTemplate<String, Object> redisTemplate,
							   SimpMessagingTemplate messagingTemplate,
							   @Autowired(required = false) FirebaseService firebaseService) {
		this.notificationRepository = notificationRepository;
		this.userRepository = userRepository;
		this.redisTemplate = redisTemplate;
		this.messagingTemplate = messagingTemplate;
		this.firebaseService = Optional.ofNullable(firebaseService);
	}

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

		Notification notification = Notification.builder()
				.recipient(recipient)
				.sender(sender)
				.type(type)
				.title(title)
				.message(message)
				.referenceId(referenceId)
				.imageUrl(imageUrl)
				.isRead(false) // Mặc định là chưa đọc
				.build();

		notificationRepository.save(notification);

		if (recipient.getFcmToken() != null && firebaseService.isPresent()) {
			Map<String, String> dataPayload = new HashMap<>();
			dataPayload.put("type", type.name());
			if (referenceId != null) dataPayload.put("referenceId", referenceId);
			if (sender != null) dataPayload.put("senderId", sender.getId());
			if (imageUrl != null) dataPayload.put("imageUrl", imageUrl);

			firebaseService.get().sendNotification(
					recipient.getFcmToken(),
					title,
					message,
					dataPayload
			);
		} else if (recipient.getFcmToken() != null && firebaseService.isEmpty()) {
			log.debug("FCM token available but Firebase service is disabled. Skipping FCM notification.");
		}

		NotificationResponse response = toResponse(notification);

		// Gửi qua WebSocket
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

		// Cập nhật trạng thái và Lưu Cứng vào DB
		notification.setRead(true);
		notificationRepository.save(notification);
	}

	@Transactional
	public void markAllAsRead(String userId) {
		// [LƯU Ý] Đảm bảo hàm này trong NotificationRepository đã có @Modifying
		notificationRepository.markAllAsRead(userId);
	}

	@Transactional(readOnly = true)
	public long countUnread(String userId) {
		// Keep legacy method name but align with current 'isSeen' badge semantics.
		return notificationRepository.countByRecipientIdAndIsSeenFalse(userId);
	}

	@Transactional(readOnly = true)
	public long countUnseen(String userId) {
		return notificationRepository.countByRecipientIdAndIsSeenFalse(userId);
	}

	@Transactional
	public void markAllAsSeen(String userId) {
		notificationRepository.markAllAsSeen(userId);
	}

	@Transactional
	public void deleteNotification(String notificationId, String userId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));

		// Phải đúng chủ nhân mới được xóa
		if (notification.getRecipient().getId().equals(userId)) {
			notificationRepository.delete(notification);
		}
	}

	@Transactional
	public void deleteAllMyNotifications(String userId) {
		// Hàm này tự viết thêm bên NotificationRepository
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
				.createdAt(n.getCreatedAt())
				.senderId(n.getSender() != null ? n.getSender().getId() : null)
				.senderName(n.getSender() != null ? n.getSender().getFullname() : "Hệ thống")
				.build();
	}
}
