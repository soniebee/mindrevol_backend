package com.mindrevol.core.modules.mood.service.impl;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.box.repository.BoxMemberRepository;
import com.mindrevol.core.modules.box.repository.BoxRepository;
import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;
import com.mindrevol.core.modules.mood.entity.Mood;
import com.mindrevol.core.modules.mood.entity.MoodReaction;
import com.mindrevol.core.modules.mood.event.MoodAskedEvent;
import com.mindrevol.core.modules.mood.event.MoodCreatedEvent;
import com.mindrevol.core.modules.mood.event.MoodReactedEvent;
import com.mindrevol.core.modules.mood.mapper.MoodMapper;
import com.mindrevol.core.modules.mood.repository.MoodReactionRepository;
import com.mindrevol.core.modules.mood.repository.MoodRepository;
import com.mindrevol.core.modules.mood.service.MoodService;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoodServiceImpl implements MoodService {

    private final MoodRepository moodRepository;
    private final MoodReactionRepository moodReactionRepository;
    private final BoxRepository boxRepository;
    private final UserRepository userRepository;
    private final BoxMemberRepository boxMemberRepository;

    private final MoodMapper moodMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request) {
        // Kiểm tra quyền thành viên
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn không phải là thành viên của Không gian này!");
        }

        LocalDateTime now = LocalDateTime.now();
        // Lấy trạng thái CÒN HẠN của user trong Box
        Optional<Mood> existingMood = moodRepository.findByBoxIdAndUserIdAndExpiresAtAfter(boxId, userId, now);

        Mood mood;
        if (existingMood.isPresent()) {
            mood = existingMood.get();
            mood.setIcon(request.getIcon());
            mood.setMessage(request.getMessage());
            mood.setExpiresAt(now.plusHours(24)); // Làm mới lại thời gian 24h

            // Clear tim cũ khi cập nhật cảm xúc mới
            moodReactionRepository.deleteAllByMoodId(mood.getId());
            if (mood.getReactions() != null) {
                mood.getReactions().clear();
            }
        } else {
            mood = Mood.builder()
                    .box(boxRepository.getReferenceById(boxId))
                    .user(userRepository.getReferenceById(userId))
                    .icon(request.getIcon())
                    .message(request.getMessage())
                    .expiresAt(now.plusHours(24))
                    .build();
        }

        mood = moodRepository.save(mood);

        // Bắn sự kiện (Push Notification / Socket)
        eventPublisher.publishEvent(MoodCreatedEvent.builder()
                .moodId(mood.getId())
                .boxId(mood.getBox().getId())
                .userId(mood.getUser().getId())
                .icon(mood.getIcon())
                .build());

        return moodMapper.toResponse(mood);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodResponse> getActiveMoodsInBox(String boxId, String userId) {
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn không có quyền xem trạng thái của Không gian này");
        }

        // Lấy danh sách mood còn hạn
        List<Mood> activeMoods = moodRepository.findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(boxId, LocalDateTime.now());

        return activeMoods.stream()
                .map(moodMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reactToMood(String boxId, String moodId, String userId, String emoji) {
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn phải là thành viên mới được thả cảm xúc");
        }

        Mood mood = moodRepository.findById(moodId)
                .orElseThrow(() -> new ResourceNotFoundException("Trạng thái không tồn tại"));

        if (mood.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Trạng thái này đã hết hạn");
        }

        // Cập nhật tim nếu đã thả, thêm mới nếu chưa
        Optional<MoodReaction> existingReaction = moodReactionRepository.findByMoodIdAndUserId(moodId, userId);

        if (existingReaction.isPresent()) {
            MoodReaction reaction = existingReaction.get();
            reaction.setEmoji(emoji);
            moodReactionRepository.save(reaction);
        } else {
            MoodReaction newReaction = MoodReaction.builder()
                    .mood(mood)
                    .user(userRepository.getReferenceById(userId))
                    .emoji(emoji)
                    .build();
            moodReactionRepository.save(newReaction);
        }

        // Chỉ bắn thông báo nếu người thả tim không phải là chủ nhân của mood
        if (!userId.equals(mood.getUser().getId())) {
            eventPublisher.publishEvent(MoodReactedEvent.builder()
                    .moodId(mood.getId())
                    .boxId(mood.getBox().getId())
                    .reactorId(userId)
                    .moodOwnerId(mood.getUser().getId())
                    .emoji(emoji)
                    .build());
        }
    }

    @Override
    @Transactional
    public void removeReaction(String moodId, String userId) {
        MoodReaction reaction = moodReactionRepository.findByMoodIdAndUserId(moodId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa thả cảm xúc vào trạng thái này"));

        moodReactionRepository.delete(reaction);
    }

    @Override
    @Transactional
    public void deleteMyMood(String boxId, String userId) {
        Mood mood = moodRepository.findByBoxIdAndUserIdAndExpiresAtAfter(boxId, userId, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không có trạng thái nào đang hoạt động"));

        moodRepository.delete(mood);
    }

    @Override
    @Transactional
    public void askFriendMood(String boxId, String askerId, String targetUserId) {
        // Kiểm tra 2 người có trong Box không
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, askerId) || 
            !boxMemberRepository.existsByBoxIdAndUserId(boxId, targetUserId)) {
            throw new BadRequestException("Cả hai phải là thành viên của Không gian này!");
        }

        if (askerId.equals(targetUserId)) {
            throw new BadRequestException("Bạn không thể tự hỏi thăm chính mình!");
        }

        // Bắn sự kiện để xử lý thông báo "A đang hỏi thăm cảm xúc của bạn"
        eventPublisher.publishEvent(MoodAskedEvent.builder()
                .boxId(boxId)
                .askerId(askerId)
                .targetUserId(targetUserId)
                .build());
    }
}