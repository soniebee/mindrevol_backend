package com.mindrevol.core.modules.journey.service.impl;

import com.mindrevol.core.common.constant.AppConstants;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.core.modules.journey.entity.*;
import com.mindrevol.core.modules.journey.event.JourneyJoinedEvent;
import com.mindrevol.core.modules.journey.mapper.JourneyInvitationMapper;
import com.mindrevol.core.modules.journey.repository.JourneyInvitationRepository;
import com.mindrevol.core.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.core.modules.journey.repository.JourneyRepository;
import com.mindrevol.core.modules.journey.repository.JourneyRequestRepository;
import com.mindrevol.core.modules.journey.service.JourneyInvitationService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourneyInvitationServiceImpl implements JourneyInvitationService {

    private final JourneyInvitationRepository invitationRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final JourneyInvitationMapper invitationMapper; // [ĐÃ CẬP NHẬT] Dùng Mapper riêng
    private final JourneyRequestRepository journeyRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void inviteFriendToJourney(String inviterId, String journeyId, String friendId) {
        // Cần truy vấn User để lấy thông tin gửi thông báo và lưu vào Invitation
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found"));

        // 1. Kiểm tra người mời có trong nhóm không
        JourneyParticipant inviterParticipant = participantRepository.findByJourneyIdAndUserId(journeyId, inviterId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this journey"));

        // 2. Kiểm tra quyền mời (Private thì chỉ Owner được mời)
        if (journey.getVisibility() == JourneyVisibility.PRIVATE) {
            if (inviterParticipant.getRole() != JourneyRole.OWNER) {
                throw new BadRequestException("Private journey: only the owner can invite members.");
            }
        }

        // 3. Kiểm tra giới hạn thành viên (Dựa trên gói của CREATOR)
        User owner = journey.getCreator();
        int limit = owner.isPremium() ? AppConstants.MAX_PARTICIPANTS_GOLD : AppConstants.MAX_PARTICIPANTS_FREE;
        
        long currentMembers = participantRepository.countByJourneyId(journeyId);
        if (currentMembers >= limit) {
            throw new BadRequestException("Journey has reached the member limit (" + limit + "). The owner needs to upgrade to Gold to expand it.");
        }

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (participantRepository.existsByJourneyIdAndUserId(journeyId, friendId)) {
            throw new BadRequestException("This user has already joined the journey");
        }

        if (invitationRepository.existsByJourneyIdAndInviteeIdAndStatus(journeyId, friendId, JourneyInvitationStatus.PENDING)) {
            throw new BadRequestException("An invitation has already been sent to this user. Please wait for their response.");
        }

        JourneyInvitation invitation = JourneyInvitation.builder()
                .journey(journey)
                .inviter(inviter)
                .invitee(friend)
                .status(JourneyInvitationStatus.PENDING)
                .build();

        invitationRepository.save(invitation);
        
        notificationService.sendAndSaveNotificationFull(
                friend.getId(),
                inviter.getId(),
                NotificationType.JOURNEY_INVITE,
                "Journey invitation: " + journey.getName(),
                inviter.getFullname() + " invited you to join: " + journey.getName(),
                journey.getId(), 
                inviter.getAvatarUrl(),
                "noti.journey.invite",
                "[\"" + inviter.getFullname() + "\",\"" + journey.getName() + "\"]",
                "PENDING"
        );
        log.info("User {} invited User {} to Journey {}", inviter.getId(), friendId, journeyId);
    }

    @Override
    @Transactional
    public void acceptInvitation(String currentUserId, String invitationId) {
        // Cần truy vấn User để thêm vào JourneyParticipant và bắn Event
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        JourneyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or not assigned to you"));

        if (invitation.getStatus() != JourneyInvitationStatus.PENDING) {
            throw new BadRequestException("This invitation has already been processed or expired");
        }

        Journey journey = invitation.getJourney();

        // Check lại giới hạn (Race condition protection)
        User owner = journey.getCreator();
        int limit = owner.isPremium() ? AppConstants.MAX_PARTICIPANTS_GOLD : AppConstants.MAX_PARTICIPANTS_FREE;
        
        long currentMembers = participantRepository.countByJourneyId(journey.getId());
        if (currentMembers >= limit) {
             throw new BadRequestException("Sorry, this journey is already full (" + limit + " members).");
        }

        // Nếu đã là thành viên -> Dọn dẹp
        if (participantRepository.existsByJourneyIdAndUserId(journey.getId(), currentUserId)) {
            invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
            cleanupPendingRequests(journey.getId(), currentUserId);
            return;
        }

        // Vào nhóm
        JourneyParticipant participant = JourneyParticipant.builder()
                .journey(journey) 
                .user(currentUser)
                .role(JourneyRole.MEMBER)
                .currentStreak(0)
                .totalCheckins(0)
                .joinedAt(LocalDateTime.now())
                .build();
        
        participantRepository.save(participant);

        invitation.setStatus(JourneyInvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        notificationService.updateActionStatusForNotification(
                currentUserId,
                NotificationType.JOURNEY_INVITE,
                journey.getId(),
                "ACCEPTED"
        );

        cleanupPendingRequests(journey.getId(), currentUserId);
        eventPublisher.publishEvent(new JourneyJoinedEvent(journey, currentUser));
        
        log.info("User {} joined Journey {} directly via invitation", currentUserId, journey.getId());
    }

    @Override
    @Transactional
    public void rejectInvitation(String currentUserId, String invitationId) {
        // [TỐI ƯU] Không cần query User ở đây nữa!
        JourneyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != JourneyInvitationStatus.PENDING) {
            throw new BadRequestException("Invalid invitation");
        }

        invitation.setStatus(JourneyInvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        notificationService.updateActionStatusForNotification(
                currentUserId,
                NotificationType.JOURNEY_INVITE,
                invitation.getJourney().getId(),
                "REJECTED"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JourneyInvitationResponse> getMyPendingInvitations(String currentUserId, Pageable pageable) {
        // [TỐI ƯU] Không cần query User ở đây nữa!
        return invitationRepository.findPendingInvitationsForUser(currentUserId, pageable)
                .map(invitationMapper::toResponse); // Dùng đúng hàm toResponse của JourneyInvitationMapper
    }

    private void cleanupPendingRequests(String journeyId, String userId) {
        List<JourneyRequest> pendingRequests = journeyRequestRepository.findAllByJourneyIdAndStatus(journeyId, RequestStatus.PENDING);
        for (JourneyRequest req : pendingRequests) {
            if (req.getUser().getId().equals(userId)) {
                req.setStatus(RequestStatus.ACCEPTED); 
                journeyRequestRepository.save(req);
            }
        }
    }
}