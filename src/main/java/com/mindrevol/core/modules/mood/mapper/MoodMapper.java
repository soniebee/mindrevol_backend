package com.mindrevol.core.modules.mood.mapper;

import com.mindrevol.core.modules.mood.dto.response.MoodReactionResponse;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;
import com.mindrevol.core.modules.mood.entity.Mood;
import com.mindrevol.core.modules.mood.entity.MoodReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MoodMapper {

    // Ánh xạ từ Mood Entity sang MoodResponse DTO
    @Mapping(source = "box.id", target = "boxId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullname", target = "fullname")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    // Các trường cùng tên như icon, message, spotifyTrackId, expiresAt, updatedAt... sẽ tự động map
    MoodResponse toResponse(Mood mood);

    // Ánh xạ từ MoodReaction Entity sang MoodReactionResponse DTO
    // MapStruct sẽ tự động dùng hàm này để map cái List<MoodReaction> ở trên
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullname", target = "fullname")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    // Trường emoji cùng tên tự động map
    MoodReactionResponse toReactionResponse(MoodReaction reaction);
}