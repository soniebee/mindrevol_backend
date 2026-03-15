package com.mindrevol.core.modules.mood.mapper;

import com.mindrevol.core.modules.mood.dto.response.MoodReactionResponse;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;
import com.mindrevol.core.modules.mood.entity.Mood;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MoodMapper {

    public MoodResponse toResponse(Mood mood) {
        if (mood == null) {
            return null;
        }

        List<MoodReactionResponse> reactionResponses = mood.getReactions().stream()
                .map(r -> MoodReactionResponse.builder()
                        .userId(r.getUser().getId())
                        .fullname(r.getUser().getFullname())
                        .avatarUrl(r.getUser().getAvatarUrl())
                        .emoji(r.getEmoji())
                        .build())
                .collect(Collectors.toList());

        return MoodResponse.builder()
                .id(mood.getId())
                .boxId(mood.getBox().getId())
                .userId(mood.getUser().getId())
                .fullname(mood.getUser().getFullname())
                .avatarUrl(mood.getUser().getAvatarUrl())
                .icon(mood.getIcon())
                .message(mood.getMessage())
                .updatedAt(mood.getUpdatedAt())
                .expiresAt(mood.getExpiresAt())
                .reactions(reactionResponses)
                .build();
    }
}