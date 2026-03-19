package com.mindrevol.core.modules.mood.service.impl;

import com.mindrevol.core.common.exception.BadRequestException;
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

    private final MoodMapper moodMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request) {
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn không ở trong Box này!");
        }

        LocalDateTime now = LocalDateTime.now();
        // 🔥 PRO MAX: Sử dụng hàm AndExpiresAtAfter để đảm bảo chỉ lôi ra trạng thái CÒN HẠN
        Optional<Mood> existingMood = moodRepository.findByBoxIdAndUserIdAndExpiresAtAfter(boxId, userId, now);

        Mood mood;
        if (existingMood.isPresent()) {
            mood = existingMood.get();
            mood.setIcon(request.getIcon());
            mood.setMessage(request.getMessage());
            mood.setExpiresAt(now.plusHours(24)); // Đếm lại 24h

            // 🔥 PRO MAX: Quét sạch tim của trạng thái cũ để tránh bị "nhận vơ"
            moodReactionRepository.deleteAllByMoodId(mood.getId());

            // 🔥 ULTIMATE: Xóa sạch cả trong bộ nhớ tạm của Hibernate để tránh lỗi đồng bộ
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

        // 📢 Bắn sự kiện tạo Mood (Để nếu cần thiết WebSocket có thể đẩy thông báo)
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
        // 🔥 PRO MAX: Chặn người ngoài xem trộm trạng thái của Box
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn không có quyền xem trạng thái của Box này");
        }

        List<Mood> activeMoods = moodRepository.findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(boxId, LocalDateTime.now());

        return activeMoods.stream()
                .map(moodMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reactToMood(String boxId, String moodId, String userId, String emoji) {
        // Kiểm tra quyền
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Phải là thành viên mới được thả cảm xúc");
        }

        Mood mood = moodRepository.findById(moodId)
                .orElseThrow(() -> new ResourceNotFoundException("Mood không tồn tại"));

        if (mood.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Trạng thái này đã hết hạn");
        }

        // 🔥 PRO MAX: Dù user có spam bấm 50 lần, Database vẫn chỉ lưu 1 dòng duy nhất!
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

        // 📢 Bắn sự kiện React: Để làm hiệu ứng "Bão Locket".
        // 🔥 ULTIMATE: Không cho phép tự tạo bão trên chính status của mình (người lạ thả thì mới bay icon)
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
        // 🔥 ULTIMATE: Tính năng gỡ cảm xúc (Hủy thả tim)
        MoodReaction reaction = moodReactionRepository.findByMoodIdAndUserId(moodId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa thả cảm xúc vào trạng thái này"));

        moodReactionRepository.delete(reaction);
    }

    @Override
    @Transactional
    public void deleteMyMood(String boxId, String userId) {
        // 🔥 ULTIMATE: Hard Delete - Tìm trạng thái hiện tại (còn hạn) và xóa thẳng tay cho nhẹ Database
        Mood mood = moodRepository.findByBoxIdAndUserIdAndExpiresAtAfter(boxId, userId, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không có trạng thái nào trong Không gian này"));

        moodRepository.delete(mood);
    }
}