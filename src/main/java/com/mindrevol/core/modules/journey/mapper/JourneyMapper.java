package com.mindrevol.core.modules.journey.mapper;

import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
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
}