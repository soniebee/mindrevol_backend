package com.mindrevol.core.modules.journey.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.journey.dto.request.InviteFriendRequest;
import com.mindrevol.core.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.core.modules.journey.service.JourneyInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/journey-invitations")
@RequiredArgsConstructor
public class JourneyInvitationController {

    private final JourneyInvitationService invitationService;

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> inviteFriend(@Valid @RequestBody InviteFriendRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        invitationService.inviteFriendToJourney(currentUserId, request.getJourneyId(), request.getFriendId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable String invitationId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        invitationService.acceptInvitation(currentUserId, invitationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(@PathVariable String invitationId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        invitationService.rejectInvitation(currentUserId, invitationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<JourneyInvitationResponse>>> getMyInvitations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        Page<JourneyInvitationResponse> invitations = invitationService.getMyPendingInvitations(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }
}