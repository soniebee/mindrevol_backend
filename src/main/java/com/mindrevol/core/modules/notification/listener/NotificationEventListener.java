package com.mindrevol.core.modules.notification.listener;

import com.mindrevol.core.common.event.CheckinSuccessEvent;
import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.box.event.BoxInvitedEvent;
import com.mindrevol.core.modules.box.event.BoxMemberAddedEvent;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.checkin.event.CheckinReactedEvent;
import com.mindrevol.core.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.journey.entity.JourneyParticipant;
import com.mindrevol.core.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.core.modules.mood.entity.Mood;
import com.mindrevol.core.modules.mood.event.MoodReactedEvent;
import com.mindrevol.core.modules.mood.repository.MoodRepository;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.service.NotificationService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CheckinRepository checkinRepository;
    private final JourneyParticipantRepository participantRepository;
    private final MoodRepository moodRepository;
    private final UserRepository userRepository;

    // BỔ SUNG SPRINT 2: Tiêm Redis Template để xử lý chống spam
    private final StringRedisTemplate redisTemplate;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_.]+)");

    // --- 1. XỬ LÝ KHI CÓ BÀI ĐĂNG MỚI (CHECK-IN) ---
    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) {
            return;
        }

        User author = checkin.getUser();
        String journeyName = checkin.getJourney().getName();
        String journeyId = checkin.getJourney().getId();

        List<JourneyParticipant> participants = participantRepository.findAllByJourneyId(journeyId);
        for (JourneyParticipant participant : participants) {
            User recipient = participant.getUser();
            if (recipient.getId().equals(author.getId())) {
                continue;
            }

            notificationService.sendAndSaveNotificationFull(
                    recipient.getId(),
                    author.getId(),
                    NotificationType.CHECKIN,
                    "Khoanh khac moi",
                    author.getFullname() + " vừa check-in trong " + journeyName,
                    checkin.getId(),
                    checkin.getImageUrl(),
                    "noti.checkin.new",
                    "[\"" + author.getFullname() + "\",\"" + journeyName + "\"]",
                    null
            );
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
            notificationService.sendAndSaveNotificationFull(
                    postOwner.getId(),
                    commenter.getId(),
                    NotificationType.COMMENT,
                    "Binh luan moi",
                    commenter.getFullname() + ": " + event.getContent(),
                    checkin.getId(),
                    commenter.getAvatarUrl(),
                    "noti.comment.new",
                    "[\"" + commenter.getFullname() + "\"]",
                    null
            );
        }

        notifyMentions(event, checkin, commenter);
    }

    // --- 3. XỬ LÝ KHI CÓ NGƯỜI ĐƯỢC THÊM VÀO BOX ---
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoxMemberAdded(BoxMemberAddedEvent event) {
        Box box = event.getBox();
        User adder = event.getAdder();
        User newMember = event.getNewMember();

        notificationService.sendAndSaveNotificationFull(
                newMember.getId(),
                adder.getId(),
                NotificationType.BOX_ADDED,
                "Khong gian moi",
                adder.getFullname() + " đã thêm bạn vào không gian " + box.getName(),
                box.getId(),
                box.getAvatar(),
                "noti.box.added",
                "[\"" + adder.getFullname() + "\",\"" + box.getName() + "\"]",
                null
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoxInvited(BoxInvitedEvent event) {
        User inviter = userRepository.findById(event.getSenderId()).orElse(null);
        User invitee = userRepository.findById(event.getRecipientId()).orElse(null);
        if (inviter == null || invitee == null) {
            return;
        }

        notificationService.sendAndSaveNotificationFull(
                invitee.getId(),
                inviter.getId(),
                NotificationType.BOX_INVITE,
                "Loi moi Khong gian",
                inviter.getFullname() + " đã mời bạn tham gia vào " + event.getBoxName(),
                event.getInvitationId(),
                inviter.getAvatarUrl(),
                "noti.box.invite",
                "[\"" + inviter.getFullname() + "\",\"" + event.getBoxName() + "\"]",
                "PENDING"
        );
    }

    // --- 4. XỬ LÝ TƯƠNG TÁC CHECK-IN (CÓ CHỐNG SPAM) ---
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCheckinReacted(CheckinReactedEvent event) {
        if (event.getCheckinOwnerId().equals(event.getReactorId())) {
            return;
        }

        // BỔ SUNG SPRINT 2 (TASK-503): Chống Spam bằng Redis Throttle
        // Key: noti_throttle:{người_nhận}:{người_gửi}:REACTION_CHECKIN:{id_bài_viết}
        String throttleKey = String.format("noti_throttle:%s:%s:REACTION_CHECKIN:%s",
                event.getCheckinOwnerId(), event.getReactorId(), event.getCheckinId());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey))) {
            log.debug("Skip throttled checkin reaction notification: {}", throttleKey);
            return;
        }

        // Lưu Cache 10 giây. Nếu user spam click liên tục trong 10s sẽ bị bỏ qua
        redisTemplate.opsForValue().set(throttleKey, "1", 10, TimeUnit.SECONDS);

        User reactor = userRepository.findById(event.getReactorId()).orElse(null);
        if (reactor == null) {
            return;
        }

        notificationService.sendAndSaveNotificationFull(
                event.getCheckinOwnerId(),
                reactor.getId(),
                NotificationType.REACTION,
                "Tương tác mới",
                reactor.getFullname() + " đã thả cảm xúc vào bài viết của bạn",
                event.getCheckinId(),
                reactor.getAvatarUrl(),
                "noti.reaction.checkin",
                "[\"" + reactor.getFullname() + "\"]",
                null
        );
    }

    // --- 5. XỬ LÝ TƯƠNG TÁC MOOD (CÓ CHỐNG SPAM) ---
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMoodReacted(MoodReactedEvent event) {
        if (event.getMoodOwnerId().equals(event.getReactorId())) {
            return;
        }

        // BỔ SUNG SPRINT 2 (TASK-503): Chống Spam bằng Redis Throttle
        // Key: noti_throttle:{người_nhận}:{người_gửi}:REACTION_MOOD:{id_mood}
        String throttleKey = String.format("noti_throttle:%s:%s:REACTION_MOOD:%s",
                event.getMoodOwnerId(), event.getReactorId(), event.getMoodId());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(throttleKey))) {
            log.debug("Skip throttled mood reaction notification: {}", throttleKey);
            return;
        }

        redisTemplate.opsForValue().set(throttleKey, "1", 10, TimeUnit.SECONDS);

        User reactor = userRepository.findById(event.getReactorId()).orElse(null);
        Mood mood = moodRepository.findById(event.getMoodId()).orElse(null);
        if (reactor == null || mood == null) {
            return;
        }

        notificationService.sendAndSaveNotificationFull(
                event.getMoodOwnerId(),
                reactor.getId(),
                NotificationType.REACTION,
                "Tương tác mới",
                reactor.getFullname() + " đã thả cảm xúc vào trạng thái của bạn",
                event.getMoodId(),
                reactor.getAvatarUrl(),
                "noti.reaction.mood",
                "[\"" + reactor.getFullname() + "\",\"" + mood.getBox().getName() + "\"]",
                null
        );
    }


    // --- 6. XỬ LÝ NHẮC TÊN TRONG BÌNH LUẬN ---
    private void notifyMentions(CommentPostedEvent event, Checkin checkin, User commenter) {
        Matcher matcher = MENTION_PATTERN.matcher(event.getContent());
        Set<String> handles = new HashSet<>();

        while (matcher.find()) {
            handles.add(matcher.group(1));
        }

        for (String handle : handles) {
            userRepository.findByHandle(handle).ifPresent(mentionedUser -> {
                if (mentionedUser.getId().equals(commenter.getId())) {
                    return;
                }

                notificationService.sendAndSaveNotificationFull(
                        mentionedUser.getId(),
                        commenter.getId(),
                        NotificationType.MOOD_MENTIONED,
                        "Bạn được nhắc đến",
                        commenter.getFullname() + " đã nhắc bạn trong một bình luận",
                        checkin.getId(),
                        commenter.getAvatarUrl(),
                        "noti.comment.mentioned",
                        "[\"" + commenter.getFullname() + "\"]",
                        null
                );
            });
        }
    }
}