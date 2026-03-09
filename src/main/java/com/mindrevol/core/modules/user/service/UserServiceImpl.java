package com.mindrevol.core.modules.user.service;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.user.dto.request.BlockUserDto;
import com.mindrevol.core.modules.user.dto.request.ChangePasswordDto;
import com.mindrevol.core.modules.user.dto.request.FollowUserDto;
import com.mindrevol.core.modules.user.dto.request.UpdateProfileDto;
import com.mindrevol.core.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.core.modules.user.dto.response.UserPublicResponse;
import com.mindrevol.core.modules.user.entity.Friendship;
import com.mindrevol.core.modules.user.entity.FriendshipStatus;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserBlock;
import com.mindrevol.core.modules.user.mapper.UserMapper;
import com.mindrevol.core.modules.user.repository.FriendshipRepository;
import com.mindrevol.core.modules.user.repository.SocialAccountRepository;
import com.mindrevol.core.modules.user.repository.UserBlockRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation xử lý các chức năng của User Module
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String userId) {
        log.info("Fetching profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return userMapper.toUserProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicResponse getUserProfile(String userId) {
        log.info("Fetching public profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return userMapper.toUserPublicResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileDto request) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        userMapper.updateUserFromDto(user, request);
        User updatedUser = userRepository.save(user);

        log.info("Profile updated successfully for user: {}", userId);
        return userMapper.toUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordDto request) {
        log.info("Changing password for user: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không trùng khớp");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void followUser(String currentUserId, FollowUserDto request) {
        log.info("User {} following user {}", currentUserId, request.getTargetUserId());

        if (currentUserId.equals(request.getTargetUserId())) {
            throw new BadRequestException("Không thể theo dõi chính mình");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + request.getTargetUserId()));

        // Tạo hoặc cập nhật Friendship record với status FOLLOWING
        Friendship friendship = friendshipRepository.findByUserIdAndFriendId(currentUserId, request.getTargetUserId())
                .orElse(Friendship.builder()
                        .user(currentUser)
                        .friend(targetUser)
                        .build());

        friendship.setStatus(FriendshipStatus.FOLLOWING);
        friendshipRepository.save(friendship);

        log.info("User {} successfully followed user {}", currentUserId, request.getTargetUserId());
    }

    @Override
    @Transactional
    public void unfollowUser(String currentUserId, String targetUserId) {
        log.info("User {} unfollowing user {}", currentUserId, targetUserId);

        friendshipRepository.deleteByUserIdAndFriendId(currentUserId, targetUserId);

        log.info("User {} successfully unfollowed user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public void blockUser(String currentUserId, BlockUserDto request) {
        log.info("User {} blocking user {}", currentUserId, request.getTargetUserId());

        if (currentUserId.equals(request.getTargetUserId())) {
            throw new BadRequestException("Không thể chặn chính mình");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + request.getTargetUserId()));

        // Tạo UserBlock record
        UserBlock userBlock = UserBlock.builder()
                .blocker(currentUser)
                .blocked(targetUser)
                .build();
        userBlockRepository.save(userBlock);

        log.info("User {} successfully blocked user {}", currentUserId, request.getTargetUserId());
    }

    @Override
    @Transactional
    public void unblockUser(String currentUserId, String targetUserId) {
        log.info("User {} unblocking user {}", currentUserId, targetUserId);

        userBlockRepository.deleteByBlockerIdAndBlockedId(currentUserId, targetUserId);

        log.info("User {} successfully unblocked user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserPublicResponse> getFollowers(String userId, Pageable pageable) {
        log.info("Fetching followers for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return friendshipRepository.findFollowersOfUser(userId, pageable)
                .map(obj -> userMapper.toUserPublicResponse((User) obj));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserPublicResponse> getFollowing(String userId, Pageable pageable) {
        log.info("Fetching following list for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return friendshipRepository.findFollowingOfUser(userId, pageable)
                .map(obj -> userMapper.toUserPublicResponse((User) obj));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserPublicResponse> searchUsers(String query, Pageable pageable) {
        log.info("Searching users with query: {}", query);

        if (query == null || query.isEmpty()) {
            return userRepository.findAll(pageable)
                    .map(userMapper::toUserPublicResponse);
        }

        return userRepository.searchByHandleOrFullname(query, pageable)
                .map(userMapper::toUserPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedAccountResponse> getLinkedAccounts(String userId) {
        log.info("Fetching linked accounts for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return socialAccountRepository.findByUserId(userId).stream()
                .map(account -> LinkedAccountResponse.builder()
                        .provider(account.getProvider())
                        .email(account.getEmail())
                        .avatarUrl(account.getAvatarUrl())
                        .connected(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void linkSocialAccount(String userId, String provider, String providerId, String email, String avatarUrl) {
        log.info("Linking social account {} for user {}", provider, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        com.mindrevol.core.modules.auth.entity.SocialAccount socialAccount = com.mindrevol.core.modules.auth.entity.SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .avatarUrl(avatarUrl)
                .build();

        socialAccountRepository.save(socialAccount);

        log.info("Social account {} linked successfully for user {}", provider, userId);
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(String userId, String provider) {
        log.info("Unlinking social account {} for user {}", provider, userId);

        socialAccountRepository.deleteByUserIdAndProvider(userId, provider);

        log.info("Social account {} unlinked successfully for user {}", provider, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFriend(String userId, String targetUserId) {
        return friendshipRepository.existsByUserIdAndFriendIdAndStatus(
                userId, targetUserId, FriendshipStatus.FRIEND);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(String userId, String targetUserId) {
        return friendshipRepository.existsByUserIdAndFriendIdAndStatus(
                userId, targetUserId, FriendshipStatus.FOLLOWING);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String userId, String targetUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getFriendshipStatus(String userId, String targetUserId) {
        if (isBlocked(userId, targetUserId)) {
            return "BLOCKED";
        }
        if (isFriend(userId, targetUserId)) {
            return "FRIEND";
        }
        if (isFollowing(userId, targetUserId)) {
            return "FOLLOW";
        }
        return "NONE";
    }
}















