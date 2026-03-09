package com.mindrevol.core.modules.user.service;

import com.mindrevol.core.modules.user.dto.request.BlockUserDto;
import com.mindrevol.core.modules.user.dto.request.ChangePasswordDto;
import com.mindrevol.core.modules.user.dto.request.FollowUserDto;
import com.mindrevol.core.modules.user.dto.request.UpdateProfileDto;
import com.mindrevol.core.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.core.modules.user.dto.response.UserPublicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface Service xử lý các chức năng của User Module
 */
public interface UserService {

    /**
     * Lấy thông tin hồ sơ của user hiện tại (đã xác thực)
     */
    UserProfileResponse getMyProfile(String userId);

    /**
     * Lấy thông tin hồ sơ công khai của user khác
     */
    UserPublicResponse getUserProfile(String userId);

    /**
     * Cập nhật thông tin hồ sơ người dùng
     */
    UserProfileResponse updateProfile(String userId, UpdateProfileDto request);

    /**
     * Thay đổi mật khẩu
     */
    void changePassword(String userId, ChangePasswordDto request);

    /**
     * Theo dõi người dùng
     */
    void followUser(String currentUserId, FollowUserDto request);

    /**
     * Hủy theo dõi người dùng
     */
    void unfollowUser(String currentUserId, String targetUserId);

    /**
     * Chặn người dùng
     */
    void blockUser(String currentUserId, BlockUserDto request);

    /**
     * Bỏ chặn người dùng
     */
    void unblockUser(String currentUserId, String targetUserId);

    /**
     * Lấy danh sách người theo dõi của user
     */
    Page<UserPublicResponse> getFollowers(String userId, Pageable pageable);

    /**
     * Lấy danh sách người đang theo dõi của user
     */
    Page<UserPublicResponse> getFollowing(String userId, Pageable pageable);

    /**
     * Tìm kiếm user theo handle hoặc fullname
     */
    Page<UserPublicResponse> searchUsers(String query, Pageable pageable);

    /**
     * Lấy danh sách tài khoản xã hội đã kết nối
     */
    List<LinkedAccountResponse> getLinkedAccounts(String userId);

    /**
     * Kết nối tài khoản xã hội
     */
    void linkSocialAccount(String userId, String provider, String providerId, String email, String avatarUrl);

    /**
     * Hủy kết nối tài khoản xã hội
     */
    void unlinkSocialAccount(String userId, String provider);

    /**
     * Kiểm tra xem 2 user có phải bạn không
     */
    boolean isFriend(String userId, String targetUserId);

    /**
     * Kiểm tra xem user có đang theo dõi không
     */
    boolean isFollowing(String userId, String targetUserId);

    /**
     * Kiểm tra xem user có bị chặn không
     */
    boolean isBlocked(String userId, String targetUserId);

    /**
     * Lấy trạng thái quan hệ giữa 2 user (FRIEND, FOLLOW, BLOCKED, NONE)
     */
    String getFriendshipStatus(String userId, String targetUserId);
}

