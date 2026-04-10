package com.mindrevol.core.modules.journey.mapper;

import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.core.modules.journey.dto.response.UserActiveJourneyResponse;
import com.mindrevol.core.modules.journey.entity.Journey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JourneyMapper {

    @Mapping(source = "creator.id", target = "creatorId")
    @Mapping(source = "box.id", target = "boxId")
    // Những trường này Service sẽ query và set riêng, nên ignore để tránh warning lúc build
    @Mapping(target = "participantCount", ignore = true)
    @Mapping(target = "currentUserStatus", ignore = true)
    @Mapping(target = "previewImages", ignore = true)
    JourneyResponse toResponse(Journey journey);

    // 🔥 MỚI: Tự động trích xuất thông tin Box khi map ra UI Profile
    @Mapping(source = "box.id", target = "boxId")
    @Mapping(source = "box.name", target = "boxName")
    @Mapping(source = "box.avatar", target = "boxAvatar")
    @Mapping(target = "memberAvatars", ignore = true)
    @Mapping(target = "totalMembers", ignore = true)
    @Mapping(target = "daysRemaining", ignore = true)
    @Mapping(target = "totalCheckins", ignore = true)
    @Mapping(target = "hasNewUpdates", ignore = true)
    @Mapping(target = "isProfileVisible", ignore = true)
    @Mapping(target = "checkins", ignore = true)
    UserActiveJourneyResponse toUserActiveJourneyResponse(Journey journey);
}