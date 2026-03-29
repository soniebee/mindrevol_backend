package com.mindrevol.core.modules.checkin.service.impl;

import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.checkin.entity.CheckinReaction;
import com.mindrevol.core.modules.checkin.event.CheckinReactedEvent;
import com.mindrevol.core.modules.checkin.mapper.ReactionMapper;
import com.mindrevol.core.modules.checkin.repository.CheckinCommentRepository;
import com.mindrevol.core.modules.checkin.repository.CheckinReactionRepository;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.checkin.service.ReactionService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final CheckinReactionRepository reactionRepository;
    private final CheckinCommentRepository commentRepository;
    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final ReactionMapper reactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void toggleReaction(String checkinId, String userId, String emoji, String mediaUrl) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        Optional<CheckinReaction> existing = reactionRepository.findByCheckinIdAndUserId(checkinId, userId);

        if (existing.isPresent()) {
            CheckinReaction reaction = existing.get();
            if (reaction.getEmoji().equals(emoji)) {
                reactionRepository.delete(reaction);
            } else {
                reaction.setEmoji(emoji);
                reaction.setMediaUrl(mediaUrl);
                reactionRepository.save(reaction);

                publishCheckinReactedEvent(checkin, userId, emoji);
            }
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            CheckinReaction newReaction = CheckinReaction.builder()
                    .checkin(checkin)
                    .user(user)
                    .emoji(emoji)
                    .mediaUrl(mediaUrl)
                    .build();
            reactionRepository.save(newReaction);

            publishCheckinReactedEvent(checkin, userId, emoji);
        }
    }

    private void publishCheckinReactedEvent(Checkin checkin, String reactorId, String emoji) {
        if (checkin.getUser().getId().equals(reactorId)) {
            return;
        }

        eventPublisher.publishEvent(CheckinReactedEvent.builder()
                .checkinId(checkin.getId())
                .reactorId(reactorId)
                .checkinOwnerId(checkin.getUser().getId())
                .emoji(emoji)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinReactionDetailResponse> getReactions(String checkinId) {
        List<CheckinReactionDetailResponse> reactions = reactionRepository.findLatestByCheckinId(checkinId, PageRequest.of(0, 50))
                .stream()
                .map(reactionMapper::toDetailResponse)
                .collect(Collectors.toList());

        List<CheckinReactionDetailResponse> comments = commentRepository.findAllByCheckinId(checkinId, PageRequest.of(0, 50))
                .stream()
                .map(reactionMapper::toDetailResponseFromComment)
                .collect(Collectors.toList());

        List<CheckinReactionDetailResponse> allActivities = new ArrayList<>();
        allActivities.addAll(reactions);
        allActivities.addAll(comments);

        return allActivities.stream()
                .sorted(Comparator.comparing(CheckinReactionDetailResponse::getCreatedAt).reversed())
                .limit(50)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinReactionDetailResponse> getPreviewReactions(String checkinId) {
        List<CheckinReactionDetailResponse> reactions = reactionRepository.findLatestByCheckinId(checkinId, PageRequest.of(0, 3))
                .stream()
                .map(reactionMapper::toDetailResponse)
                .collect(Collectors.toList());

        List<CheckinReactionDetailResponse> comments = commentRepository.findAllByCheckinId(checkinId, PageRequest.of(0, 3))
                .stream()
                .map(reactionMapper::toDetailResponseFromComment)
                .collect(Collectors.toList());

        List<CheckinReactionDetailResponse> all = new ArrayList<>();
        all.addAll(reactions);
        all.addAll(comments);
        
        return all.stream()
                .sorted(Comparator.comparing(CheckinReactionDetailResponse::getCreatedAt).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }
}