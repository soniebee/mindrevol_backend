package com.mindrevol.core.modules.notification.listener;

import com.mindrevol.core.common.event.CheckinSuccessEvent;
import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.box.event.BoxMemberAddedEvent;
import com.mindrevol.core.modules.box.event.BoxMemberInvitedEvent;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.journey.entity.JourneyParticipant;
import com.mindrevol.core.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.service.FirebaseService;
import com.mindrevol.core.modules.notification.service.NotificationService;
import com.mindrevol.core.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final FirebaseService firebaseService;
    private final CheckinRepository checkinRepository;
    private final JourneyParticipantRepository participantRepository;

    // --- 1. XỬ LÝ KHI CÓ BÀI ĐĂNG MỚI (CHECK-IN) ---
    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        log.info("🔔 Processing Notification for Checkin: {}", event.getCheckinId());

        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        User author = checkin.getUser();
        String journeyName = checkin.getJourney().getName();
        String journeyId = checkin.getJourney().getId();

        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);

        for (JourneyParticipant p : participants) {
            User recipient = p.getUser();

            if (recipient.getId().equals(author.getId())) continue;

            String title = "Khoảnh khắc mới! 📸";
            String body = author.getFullname() + " vừa check-in trong " + journeyName;

            notificationService.sendAndSaveNotification(
                    recipient.getId(),
                    author.getId(),
                    NotificationType.CHECKIN,
                    title,
                    body,
                    checkin.getId(),
                    checkin.getImageUrl()
            );

            if (recipient.getFcmToken() != null) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "CHECKIN");
                data.put("targetId", checkin.getId());
                data.put("journeyId", journeyId);

                firebaseService.sendNotification(recipient.getFcmToken(), title, body, data);
            }
        }
    }

    // --- 2. XỬ LÝ KHI CÓ BÌNH LUẬN MỚI ---
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentPosted(CommentPostedEvent event) {
        Checkin checkin = event.getCheckin();
        User commenter = event.getCommenter();
        User postOwner = checkin.getUser();

        if (!postOwner.getId().equals(commenter.getId())) {

            String title = "Bình luận mới 💬";
            String body = commenter.getFullname() + ": " + event.getContent();

            notificationService.sendAndSaveNotification(
                    postOwner.getId(),
                    commenter.getId(),
                    NotificationType.COMMENT,
                    title,
                    body,
                    checkin.getId(),
                    commenter.getAvatarUrl()
            );

            if (postOwner.getFcmToken() != null) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "COMMENT");
                data.put("targetId", checkin.getId());

                firebaseService.sendNotification(postOwner.getFcmToken(), title, body, data);
            }

            log.info("Sent notification for comment on checkin {}", checkin.getId());
        }
    }

    // --- [THÊM MỚI] 3. XỬ LÝ KHI CÓ NGƯỜI ĐƯỢC THÊM VÀO BOX ---
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoxMemberAdded(BoxMemberAddedEvent event) {
        Box box = event.getBox();
        User adder = event.getAdder();
        User newMember = event.getNewMember();

        String title = "Không gian mới! 📦";
        String body = adder.getFullname() + " đã thêm bạn vào không gian " + box.getName();

        // 1. Lưu DB và gửi qua WebSocket
        notificationService.sendAndSaveNotification(
                newMember.getId(),
                adder.getId(),
                NotificationType.BOX_ADDED,
                title,
                body,
                box.getId(), // ReferenceId là BoxId để khi click vào sẽ bay tới Box
                box.getAvatar() != null ? box.getAvatar() : "📦" // Lấy Emoji của box làm icon thông báo
        );

        // 2. Bắn Push Notification FCM
        if (newMember.getFcmToken() != null) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "BOX_ADDED");
            data.put("targetId", box.getId());

            firebaseService.sendNotification(newMember.getFcmToken(), title, body, data);
        }

        log.info("Sent notification: {} added {} to Box {}", adder.getId(), newMember.getId(), box.getId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoxMemberInvited(BoxMemberInvitedEvent event) {
        Box box = event.getBox();
        User inviter = event.getInviter();
        User invitee = event.getInvitee();

        String title = "Lời mời Không gian! 📦";
        String body = inviter.getFullname() + " đã mời bạn tham gia vào " + box.getName();

        // Lưu DB với Type BOX_INVITE
        notificationService.sendAndSaveNotification(
                invitee.getId(),
                inviter.getId(),
                NotificationType.BOX_INVITE,
                title,
                body,
                box.getId(), // ReferenceId là BoxId
                box.getAvatar() != null ? box.getAvatar() : "📦"
        );

        // Push Notification FCM
        if (invitee.getFcmToken() != null) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "BOX_INVITE");
            data.put("targetId", box.getId());

            firebaseService.sendNotification(invitee.getFcmToken(), title, body, data);
        }

        log.info("Sent invite notification: {} invited {} to Box {}", inviter.getId(), invitee.getId(), box.getId());
    }
}