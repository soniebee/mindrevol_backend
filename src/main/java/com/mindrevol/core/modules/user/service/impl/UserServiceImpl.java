package com.mindrevol.core.modules.user.service.impl;

import com.mindrevol.core.common.service.SanitizationService;
import com.mindrevol.core.modules.auth.entity.SocialAccount;
import com.mindrevol.core.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.journey.mapper.JourneyMapper;
import com.mindrevol.core.modules.journey.repository.JourneyRepository;
import com.mindrevol.core.modules.storage.service.FileStorageService;
import com.mindrevol.core.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.core.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.core.modules.user.dto.response.UserDataExport;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.core.modules.user.entity.UserSettings;
import com.mindrevol.core.modules.user.mapper.FriendshipMapper;
import com.mindrevol.core.modules.user.repository.UserSettingsRepository;
import com.mindrevol.core.modules.user.service.UserService;
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
import com.mindrevol.core.modules.auth.repository.SocialAccountRepository;
import com.mindrevol.core.modules.user.repository.UserBlockRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SanitizationService sanitizationService;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipMapper friendshipMapper;
    private final JourneyRepository journeyRepository;
    private final JourneyMapper journeyMapper;
    private final UserBlockRepository userBlockRepository;
    private final FileStorageService fileStorageService;
    
    private final UserSettingsRepository userSettingsRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    public UserProfileResponse getMyProfile(String currentEmail) {
        User user = getUserByEmail(currentEmail);
        return buildUserProfile(user, user);
    }

    @Override
    public UserProfileResponse getPublicProfile(String handle, String currentUserEmail) {
        User targetUser = userRepository.findByHandle(handle)
                .orElseThrow(() -> new ResourceNotFoundException("User @" + handle + " does not exist."));
        return getPublicProfileCommon(targetUser, currentUserEmail);
    }

    @Override
    public UserProfileResponse getPublicProfileById(String userId, String currentUserEmail) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist."));
        return getPublicProfileCommon(targetUser, currentUserEmail);
    }

    private UserProfileResponse getPublicProfileCommon(User targetUser, String currentUserEmail) {
        User currentUser = null;
        if (currentUserEmail != null && !currentUserEmail.equals("anonymousUser")) {
            currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }
        return buildUserProfile(targetUser, currentUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request, MultipartFile file) {
        User user = getUserByEmail(currentEmail);
        
        if (file != null && !file.isEmpty()) {
            try {
                FileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file, "avatars/" + user.getId());
                user.setAvatarUrl(uploadResult.getUrl());
            } catch (Exception e) {
                log.error("Failed to upload avatar", e);
                throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
            }
        }

        if (request.getFullname() != null) request.setFullname(sanitizationService.sanitizeStrict(request.getFullname()));
        if (request.getBio() != null) request.setBio(sanitizationService.sanitizeRichText(request.getBio()));
        
        if (request.getHandle() != null && !request.getHandle().equals(user.getHandle())) {
            if (userRepository.existsByHandle(request.getHandle())) 
                throw new BadRequestException("Handle @" + request.getHandle() + " is already in use.");
        }
        
        if (request.getTimezone() != null && !request.getTimezone().isEmpty()) {
            try { java.time.ZoneId.of(request.getTimezone()); user.setTimezone(request.getTimezone()); } catch (Exception e) {}
        }
        
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        userMapper.updateUserFromRequest(request, user);
        return buildUserProfile(userRepository.save(user), user);
    }

    @Override
    @Transactional
    public void updateFcmToken(String userId, String token) { 
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(token);
        userRepository.save(user);
    }

    @Override
    public User getUserById(String id) { 
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void deleteMyAccount(String userId) { 
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 1. Đổi email và handle
        long timestamp = System.currentTimeMillis();
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);
        user.setHandle(user.getHandle() + "_deleted_" + timestamp);
        
        // [QUAN TRỌNG NHẤT]: Ép Hibernate phải cập nhật Email xuống Database ngay lập tức!
        userRepository.saveAndFlush(user); 
        
        // 2. Xóa liên kết mạng xã hội
        List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(userId);
        socialAccountRepository.deleteAll(socialAccounts);
        
        // 3. Xóa user (Kích hoạt @SQLDelete)
        userRepository.delete(user);
    }

    @Override
    public UserDataExport exportMyData(String userId) { 
        User user = getUserById(userId);

        var checkins = checkinRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(checkinMapper::toResponse).collect(Collectors.toList());

        var friends = friendshipRepository.findAllAcceptedFriendsList(userId).stream()
                .map(f -> friendshipMapper.toResponse(f, userId)) 
                .collect(Collectors.toList());

        return UserDataExport.builder()
                .profile(buildUserProfile(user, user))
                .checkins(checkins)
                .friends(friends)
                .build();
    }

    @Override
    public List<UserSummaryResponse> searchUsers(String query, String currentUserId) {
        String cleanedQuery = query.startsWith("@") ? query.substring(1) : query;
        List<User> users = userRepository.searchUsers(cleanedQuery);
        return users.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(u -> UserSummaryResponse.builder()
                        .id(u.getId())
                        .fullname(u.getFullname())
                        .handle(u.getHandle())
                        .avatarUrl(u.getAvatarUrl())
                        .friendshipStatus("NONE") 
                        .build())
                .collect(Collectors.toList());
    }

    // Helper: Lấy ngày hiện tại theo Timezone của User
    private LocalDate getTodayInUserTimezone(User user) {
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        try {
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of("UTC"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JourneyResponse> getUserRecaps(String userId) { 
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // [FIX] Lấy ngày hiện tại theo múi giờ của user để truyền vào repository
        LocalDate today = getTodayInUserTimezone(user);
        
        List<Journey> completedJourneys = journeyRepository.findCompletedJourneysByUserId(userId, today);
        return completedJourneys.stream()
                .map(journeyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserSettings getNotificationSettings(String userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = getUserById(userId);
                    UserSettings settings = UserSettings.builder().user(user).build();
                    return userSettingsRepository.save(settings);
                });
    }

    @Override
    @Transactional
    public UserSettings updateNotificationSettings(String userId, UpdateNotificationSettingsRequest request) {
        UserSettings settings = getNotificationSettings(userId);

        if (request.getEmailDailyReminder() != null) settings.setEmailDailyReminder(request.getEmailDailyReminder());
        if (request.getEmailUpdates() != null) settings.setEmailUpdates(request.getEmailUpdates());
        if (request.getPushFriendRequest() != null) settings.setPushFriendRequest(request.getPushFriendRequest());
        if (request.getPushNewComment() != null) settings.setPushNewComment(request.getPushNewComment());
        if (request.getPushJourneyInvite() != null) settings.setPushJourneyInvite(request.getPushJourneyInvite());
        if (request.getPushReaction() != null) settings.setPushReaction(request.getPushReaction());

        return userSettingsRepository.save(settings);
    }

    @Override
    @Transactional
    public void createDefaultSettings(User user) {
        if (userSettingsRepository.findByUserId(user.getId()).isEmpty()) {
            userSettingsRepository.save(UserSettings.builder().user(user).build());
        }
    }

    @Override
    public List<LinkedAccountResponse> getLinkedAccounts(String userId) {
        List<SocialAccount> accounts = socialAccountRepository.findAllByUserId(userId);
        return accounts.stream()
                .map(acc -> LinkedAccountResponse.builder()
                        .provider(acc.getProvider())
                        .email(acc.getEmail())
                        .avatarUrl(acc.getAvatarUrl())
                        .connected(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(String userId, String provider) {
        User user = getUserById(userId);
        
        boolean hasPassword = "LOCAL".equals(user.getAuthProvider());
        long socialCount = socialAccountRepository.countByUserId(userId);

        if (!hasPassword && socialCount <= 1) {
            throw new BadRequestException("You cannot unlink your only login method. Please create a password first.");
        }

        SocialAccount account = socialAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new ResourceNotFoundException("No linked account for " + provider));
        
        socialAccountRepository.delete(account);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse buildUserProfile(User targetUser, User viewer) {
        UserProfileResponse response = userMapper.toProfileResponse(targetUser);
        
        long friendCount = friendshipRepository.countByUserIdAndStatusAccepted(targetUser.getId());
        response.setFriendCount(friendCount); 

        if (viewer != null && viewer.getId().equals(targetUser.getId())) {
            response.setMe(true);
            response.setFriendshipStatus(FriendshipStatus.NONE); 
            response.setBlockedByMe(false);
            response.setBlockedByThem(false);
        } else if (viewer != null) {
            response.setMe(false);
            boolean isBlockedByMe = userBlockRepository.existsByBlockerIdAndBlockedId(viewer.getId(), targetUser.getId());
            boolean isBlockedByThem = userBlockRepository.existsByBlockerIdAndBlockedId(targetUser.getId(), viewer.getId());
            response.setBlockedByMe(isBlockedByMe);
            response.setBlockedByThem(isBlockedByThem);
            Optional<Friendship> friendship = friendshipRepository.findByUsers(viewer.getId(), targetUser.getId());
            response.setFriendshipStatus(friendship.map(Friendship::getStatus).orElse(FriendshipStatus.NONE));
        } else {
            response.setMe(false);
            response.setFriendshipStatus(FriendshipStatus.NONE);
            response.setBlockedByMe(false);
            response.setBlockedByThem(false);
        }

        response.setFollowerCount(0); 
        response.setFollowingCount(0);
        response.setFollowedByCurrentUser(false);
        
        return response;
    }
}











