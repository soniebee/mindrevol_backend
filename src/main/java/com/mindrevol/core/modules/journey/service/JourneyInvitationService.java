package com.mindrevol.core.modules.journey.service;

import com.mindrevol.core.modules.journey.dto.response.JourneyInvitationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JourneyInvitationService {
    void inviteFriendToJourney(String inviterId, String journeyId, String friendId);
    void acceptInvitation(String currentUserId, String invitationId);
    void rejectInvitation(String currentUserId, String invitationId);
    Page<JourneyInvitationResponse> getMyPendingInvitations(String currentUserId, Pageable pageable);
}