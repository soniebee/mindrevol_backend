package com.mindrevol.core.modules.user.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.user.dto.request.BlockUserDto;
import com.mindrevol.core.modules.user.dto.request.ChangePasswordDto;
import com.mindrevol.core.modules.user.dto.request.FollowUserDto;
import com.mindrevol.core.modules.user.dto.request.UpdateProfileDto;
import com.mindrevol.core.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.core.modules.user.dto.response.UserPublicResponse;
import com.mindrevol.core.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các API liên quan đến User Profile và Social Features
 *
 * Endpoints:
 * - GET  /api/v1/users/me              - Lấy thông tin profile của user hiện tại
 * - GET  /api/v1/users/{id}            - Lấy thông tin profile công khai của user
 * - PUT  /api/v1/users/me              - Cập nhật thông tin profile
 * - POST /api/v1/users/change-password - Thay đổi mật khẩu
 * - POST /api/v1/users/follow/{id}     - Theo dõi user
 * - DELETE /api/v1/users/follow/{id}   - Hủy theo dõi user
 * - POST /api/v1/users/block/{id}      - Chặn user
 * - DELETE /api/v1/users/block/{id}    - Bỏ chặn user
 * - GET  /api/v1/users/{id}/followers  - Lấy danh sách followers
 * - GET  /api/v1/users/{id}/following  - Lấy danh sách following
 * - GET  /api/v1/users/search          - Tìm kiếm user
 * - GET  /api/v1/users/me/linked-accounts - Lấy danh sách tài khoản xã hội
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile", description = "Quản lý thông tin người dùng và các chức năng xã hội")
public class UserController {

    private final UserService userService;

    // =====================================================
    // PROFILE MANAGEMENT
    // =====================================================

    /**
     * GET /api/v1/users/me
     * Lấy thông tin hồ sơ của user hiện tại (đã xác thực)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy thông tin hồ sơ của tôi")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching profile for current user: {}", userId);

        UserProfileResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/users/{id}
     * Lấy thông tin hồ sơ công khai của user
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin hồ sơ công khai của user")
    public ResponseEntity<ApiResponse<UserPublicResponse>> getUserProfile(
            @PathVariable String id) {
        log.info("Fetching public profile for user: {}", id);

        UserPublicResponse response = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/users/me
     * Cập nhật thông tin hồ sơ người dùng
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật thông tin hồ sơ của tôi")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileDto request) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Updating profile for user: {}", userId);

        UserProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thông tin hồ sơ đã được cập nhật thành công"));
    }

    // =====================================================
    // ACCOUNT SECURITY
    // =====================================================

    /**
     * POST /api/v1/users/change-password
     * Thay đổi mật khẩu
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Thay đổi mật khẩu")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordDto request) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Changing password for user: {}", userId);

        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mật khẩu đã được thay đổi thành công"));
    }

    // =====================================================
    // FOLLOW / UNFOLLOW
    // =====================================================

    /**
     * POST /api/v1/users/follow/{id}
     * Theo dõi user
     */
    @PostMapping("/follow/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Theo dõi user")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @PathVariable String id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} following user {}", currentUserId, id);

        userService.followUser(currentUserId, FollowUserDto.builder().targetUserId(id).build());
        return ResponseEntity.ok(ApiResponse.success(null, "Theo dõi thành công"));
    }

    /**
     * DELETE /api/v1/users/follow/{id}
     * Hủy theo dõi user
     */
    @DeleteMapping("/follow/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy theo dõi user")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable String id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} unfollowing user {}", currentUserId, id);

        userService.unfollowUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Hủy theo dõi thành công"));
    }

    // =====================================================
    // BLOCK / UNBLOCK
    // =====================================================

    /**
     * POST /api/v1/users/block/{id}
     * Chặn user
     */
    @PostMapping("/block/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chặn user")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable String id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} blocking user {}", currentUserId, id);

        userService.blockUser(currentUserId, BlockUserDto.builder().targetUserId(id).build());
        return ResponseEntity.ok(ApiResponse.success(null, "Chặn thành công"));
    }

    /**
     * DELETE /api/v1/users/block/{id}
     * Bỏ chặn user
     */
    @DeleteMapping("/block/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bỏ chặn user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable String id) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} unblocking user {}", currentUserId, id);

        userService.unblockUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bỏ chặn thành công"));
    }

    // =====================================================
    // FOLLOWERS / FOLLOWING
    // =====================================================

    /**
     * GET /api/v1/users/{id}/followers
     * Lấy danh sách followers của user
     */
    @GetMapping("/{id}/followers")
    @Operation(summary = "Lấy danh sách người theo dõi")
    public ResponseEntity<ApiResponse<Page<UserPublicResponse>>> getFollowers(
            @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching followers for user: {}", id);

        Page<UserPublicResponse> response = userService.getFollowers(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/users/{id}/following
     * Lấy danh sách following của user
     */
    @GetMapping("/{id}/following")
    @Operation(summary = "Lấy danh sách đang theo dõi")
    public ResponseEntity<ApiResponse<Page<UserPublicResponse>>> getFollowing(
            @PathVariable String id,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching following list for user: {}", id);

        Page<UserPublicResponse> response = userService.getFollowing(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // SEARCH
    // =====================================================

    /**
     * GET /api/v1/users/search
     * Tìm kiếm user theo handle hoặc fullname
     */
    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm user")
    public ResponseEntity<ApiResponse<Page<UserPublicResponse>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String query,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Searching users with query: {}", query);

        Page<UserPublicResponse> response = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // LINKED ACCOUNTS (SOCIAL LOGIN)
    // =====================================================

    /**
     * GET /api/v1/users/me/linked-accounts
     * Lấy danh sách tài khoản xã hội đã kết nối
     */
    @GetMapping("/me/linked-accounts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách tài khoản xã hội đã kết nối")
    public ResponseEntity<ApiResponse<List<LinkedAccountResponse>>> getLinkedAccounts() {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching linked accounts for user: {}", userId);

        List<LinkedAccountResponse> response = userService.getLinkedAccounts(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/users/me/linked-accounts/{provider}
     * Kết nối tài khoản xã hội (sẽ được xử lý từ auth service)
     */
    @PostMapping("/me/linked-accounts/{provider}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Kết nối tài khoản xã hội")
    public ResponseEntity<ApiResponse<Void>> linkSocialAccount(
            @PathVariable String provider,
            @RequestBody LinkedAccountResponse request) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Linking social account {} for user {}", provider, userId);

        userService.linkSocialAccount(userId, provider, request.getProvider(),
                request.getEmail(), request.getAvatarUrl());
        return ResponseEntity.ok(ApiResponse.success(null, "Tài khoản xã hội đã được kết nối"));
    }

    /**
     * DELETE /api/v1/users/me/linked-accounts/{provider}
     * Hủy kết nối tài khoản xã hội
     */
    @DeleteMapping("/me/linked-accounts/{provider}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy kết nối tài khoản xã hội")
    public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(
            @PathVariable String provider) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Unlinking social account {} for user {}", provider, userId);

        userService.unlinkSocialAccount(userId, provider);
        return ResponseEntity.ok(ApiResponse.success(null, "Tài khoản xã hội đã được hủy kết nối"));
    }
}

