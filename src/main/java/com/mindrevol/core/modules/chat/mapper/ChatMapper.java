package com.mindrevol.core.modules.chat.mapper;

import com.mindrevol.core.modules.chat.dto.response.MessageResponse;
import com.mindrevol.core.modules.chat.entity.Message;
import com.mindrevol.core.modules.chat.entity.MessageReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.fullname") // Xử lý warning thiếu senderName
    @Mapping(target = "senderAvatar", source = "sender.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "isDeleted", source = "deleted") 
    @Mapping(target = "replyToMsgId", source = "replyToMsgId")
    @Mapping(target = "isPinned", ignore = true) // Bỏ qua warning thiếu isPinned nếu chưa có logic
    MessageResponse toResponse(Message message);

    // Hàm tùy chỉnh giúp MapStruct map List<MessageReaction> sang Map<String, String>
    default Map<String, String> mapReactions(List<MessageReaction> reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return Collections.emptyMap();
        }
        // Chuyển đổi List thành Map với Key = userId, Value = reactionType
        return reactions.stream()
                .collect(Collectors.toMap(
                        reaction -> String.valueOf(reaction.getUser().getId()), // Đảm bảo key luôn là String
                        MessageReaction::getReactionType,
                        // Trong trường hợp 1 user có nhiều reaction (do lỗi data), lấy cái mới nhất
                        (existing, replacement) -> replacement 
                ));
    }
}