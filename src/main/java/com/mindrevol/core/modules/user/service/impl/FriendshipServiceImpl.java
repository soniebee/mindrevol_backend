package com.mindrevol.core.modules.user.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.service.NotificationService;
import com.mindrevol.core.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.core.modules.user.entity.Friendship;
import com.mindrevol.core.modules.user.entity.FriendshipStatus;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.mapper.FriendshipMapper;
import com.mindrevol.core.modules.user.repository.FriendshipRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import com.mindrevol.core.modules.user.service.FriendshipService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final FriendshipMapper friendshipMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendshipResponse sendFriendRequest(String requesterId, String targetUserId) { 
        if (requesterId.equals(targetUserId)) {
            throw new BadRequestException("Không thể tự kết bạn với chính mình");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Người gửi không tồn tại"));
        User addressee = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người nhận không tồn tại"));

        if (friendshipRepository.existsByUsers(requesterId, targetUserId)) {
            throw new BadRequestException("Đã tồn tại mối quan hệ hoặc lời mời giữa hai người");
        }

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);

        notificationService.sendAndSaveNotification(
                addressee.getId(),
                requester.getId(),
                NotificationType.FRIEND_REQUEST,
                "Lời mời kết bạn mới 👋",
                requester.getFullname() + " muốn kết bạn với bạn.",
                saved.getId(), 
                requester.getAvatarUrl()
        );

        return friendshipMapper.toResponse(saved, requesterId);
    }

    @Override
    @Transactional
    public FriendshipResponse acceptFriendRequest(String userId, String friendshipId) { 
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền chấp nhận lời mời này");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BadRequestException("Lời mời không còn hiệu lực");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        Friendship saved = friendshipRepository.save(friendship);

        User accepter = friendship.getAddressee();
        User requester = friendship.getRequester();

        notificationService.sendAndSaveNotification(
                requester.getId(),
                accepter.getId(),
                NotificationType.FRIEND_ACCEPTED,
                "Đã trở thành bạn bè 🤝",
                accepter.getFullname() + " đã chấp nhận lời mời kết bạn.",
                accepter.getId(),
                accepter.getAvatarUrl()
        );

        return friendshipMapper.toResponse(saved, userId);
    }

    @Override
    @Transactional
    public void declineFriendRequest(String userId, String friendshipId) { 
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không tồn tại"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền từ chối lời mời này");
        }
        friendshipRepository.delete(friendship);
    }

    @Override
    @Transactional
    public void removeFriendship(String userId, String targetUserId) { 
        Friendship friendship = friendshipRepository.findByUsers(userId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mối quan hệ bạn bè"));
        friendshipRepository.delete(friendship);
    }

    @Override
    public Page<FriendshipResponse> getMyFriends(String userId, Pageable pageable) { 
        return friendshipRepository.findAllAcceptedFriends(userId, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }

    // [MỚI] Hàm lấy bạn bè của một user bất kỳ (Public)
    @Override
    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getUserFriends(String userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        // Dùng lại hàm query tìm bạn đã accepted
        return friendshipRepository.findAllAcceptedFriends(userId, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getIncomingRequests(String userId, Pageable pageable) { 
        return friendshipRepository.findIncomingRequests(userId, FriendshipStatus.PENDING, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }

    @Override
    public Page<FriendshipResponse> getOutgoingRequests(String userId, Pageable pageable) { 
        return friendshipRepository.findOutgoingRequests(userId, pageable)
                .map(friendship -> friendshipMapper.toResponse(friendship, userId));
    }
}