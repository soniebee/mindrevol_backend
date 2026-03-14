package com.mindrevol.core.modules.mood.service.impl;

import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.box.repository.BoxMemberRepository;
import com.mindrevol.core.modules.box.repository.BoxRepository;
import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;
import com.mindrevol.core.modules.mood.entity.Mood;
import com.mindrevol.core.modules.mood.entity.MoodReaction;
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

    // Đã tiêm Mapper và EventPublisher vào đây
    private final MoodMapper moodMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request) {
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new RuntimeException("Bạn không ở trong Box này!");
        }

        Optional<Mood> existingMood = moodRepository.findByBoxIdAndUserId(boxId, userId);

        Mood mood;
        if (existingMood.isPresent()) {
            mood = existingMood.get();
            mood.setIcon(request.getIcon());
            mood.setMessage(request.getMessage());
            mood.setExpiresAt(LocalDateTime.now().plusHours(24)); // Đếm lại 24h
        } else {
            mood = Mood.builder()
                    .box(boxRepository.getReferenceById(boxId))
                    .user(userRepository.getReferenceById(userId))
                    .icon(request.getIcon())
                    .message(request.getMessage())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
        }

        mood = moodRepository.save(mood);

        // 📢 Phát sự kiện (Không cần dính dáng logic thông báo ở đây)
        eventPublisher.publishEvent(MoodCreatedEvent.builder()
                .moodId(mood.getId())
                .boxId(mood.getBox().getId())
                .userId(mood.getUser().getId())
                .icon(mood.getIcon())
                .build());

        // Gọi qua Mapper riêng
        return moodMapper.toResponse(mood);
    }

    @Override
    public List<MoodResponse> getActiveMoodsInBox(String boxId) {
        List<Mood> activeMoods = moodRepository.findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(boxId, LocalDateTime.now());

        return activeMoods.stream()
                .map(moodMapper::toResponse) // Gọi qua Mapper riêng
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reactToMood(String moodId, String userId, String emoji) {
        Mood mood = moodRepository.findById(moodId)
                .orElseThrow(() -> new ResourceNotFoundException("Mood không tồn tại hoặc đã hết hạn"));

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

        // 📢 Phát sự kiện (Chỉ phát nếu không phải tự thả tim cho chính mình)
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
}