package com.mindrevol.core.modules.journey.mapper;

import com.mindrevol.core.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.core.modules.journey.entity.JourneyInvitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JourneyInvitationMapper {

    @Mapping(source = "journey.id", target = "journeyId")
    @Mapping(source = "journey.name", target = "journeyName")
    @Mapping(source = "inviter.fullname", target = "inviterName")
    @Mapping(source = "inviter.avatarUrl", target = "inviterAvatar")
    @Mapping(source = "journey.status", target = "journeyStatus")
    @Mapping(source = "createdAt", target = "sentAt")
    JourneyInvitationResponse toResponse(JourneyInvitation invitation);
}