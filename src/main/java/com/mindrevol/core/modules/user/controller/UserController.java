package com.mindrevol.core.modules.user.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.checkin.dto.response.CalendarRecapResponse;
import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.core.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.core.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.core.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.core.modules.user.dto.response.NotificationSettingsResponse;
import com.mindrevol.core.modules.user.dto.response.UserDataExport;
import com.mindrevol.core.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserSettings;
import com.mindrevol.core.modules.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    @GetMapping("/{userId}/calendar")
    @Operation(summary = "Lấy dữ liệu cuốn lịch Recap tháng")
    public ResponseEntity<ApiResponse<List<CalendarRecapResponse>>> getActiveCalendar(
            @PathVariable String userId,
            @RequestParam int year,
            @RequestParam int month) {
        
        // Gọi qua tầng Service chuẩn kiến trúc
        List<CalendarRecapResponse> calendar = userService.getUserCalendarRecap(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(calendar));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        UserProfileResponse profile = userService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/handle/{handle}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getPublicProfileByHandle(
            @PathVariable String handle,
            Authentication authentication) {
        String currentEmail = (authentication != null && authentication.isAuthenticated()) 
                              ? authentication.getName() 
                              : null;
        UserProfileResponse profile = userService.getPublicProfile(handle, currentEmail);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getPublicProfileById(
            @PathVariable String id,
            Authentication authentication) {
        String currentEmail = (authentication != null && authentication.isAuthenticated()) 
                              ? authentication.getName() 
                              : null;
        UserProfileResponse profile = userService.getPublicProfileById(id, currentEmail);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // [FIX LỖI 500] Chuyển sang nhận Multipart Form Data
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            Authentication authentication,
            @ModelAttribute @Valid UpdateProfileRequest request, // Dùng ModelAttribute để map form fields
            @RequestParam(value = "file", required = false) MultipartFile file // Nhận file riêng
    ) {
        UserProfileResponse updatedProfile = userService.updateProfile(authentication.getName(), request, file);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile));
    }
    
    @PatchMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        String token = body.get("token");
        userService.updateFcmToken(currentUser.getId(), token);
        return ResponseEntity.ok(ApiResponse.success("Updated FCM Token"));
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userService.deleteMyAccount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được xóa vĩnh viễn"));
    }
    
    @GetMapping("/me/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDataExport>> exportData() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        UserDataExport data = userService.exportMyData(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> searchUsers(@RequestParam String query) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSummaryResponse> results = userService.searchUsers(query, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
    
    @GetMapping("/{userId}/recaps")
    @Operation(summary = "Lấy danh sách các hành trình đã hoàn thành (Album kỷ niệm)")
    public ResponseEntity<ApiResponse<List<JourneyResponse>>> getUserRecaps(@PathVariable String userId) {
        List<JourneyResponse> recaps = userService.getUserRecaps(userId);
        return ResponseEntity.ok(ApiResponse.success(recaps));
    }

    @GetMapping("/me/social-accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LinkedAccountResponse>>> getLinkedAccounts() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getLinkedAccounts(userId)));
    }

    @DeleteMapping("/me/social-accounts/{provider}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(@PathVariable String provider) {
        String userId = SecurityUtils.getCurrentUserId();
        userService.unlinkSocialAccount(userId, provider.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy liên kết tài khoản " + provider));
    }

    @GetMapping({"/me/notification-settings", "/settings/notifications"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getMyNotificationSettings() {
        String userId = SecurityUtils.getCurrentUserId();
        UserSettings settings = userService.getNotificationSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(toNotificationSettingsResponse(settings)));
    }

    @RequestMapping(value = {"/me/notification-settings", "/settings/notifications"}, method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateMyNotificationSettings(
            @RequestBody UpdateNotificationSettingsRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        if (request == null) {
            request = new UpdateNotificationSettingsRequest();
        }
        UserSettings settings = userService.updateNotificationSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success(toNotificationSettingsResponse(settings)));
    }

    @DeleteMapping({"/me/notification-settings", "/settings/notifications"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> resetMyNotificationSettings() {
        String userId = SecurityUtils.getCurrentUserId();
        UserSettings settings = userService.resetNotificationSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(toNotificationSettingsResponse(settings)));
    }

    private NotificationSettingsResponse toNotificationSettingsResponse(UserSettings settings) {
        return NotificationSettingsResponse.builder()
                .commentEnabled(settings.isInAppComment() || settings.isPushComment() || settings.isPushNewComment() || settings.isEmailComment())
                .reactionEnabled(settings.isInAppReaction() || settings.isPushReaction() || settings.isEmailReaction())
                .messageEnabled(settings.isInAppMessage() || settings.isPushMessage() || settings.isEmailMessage())
                .journeyEnabled(settings.isInAppJourney() || settings.isPushJourney() || settings.isPushJourneyInvite() || settings.isEmailJourney())
                .friendRequestEnabled(settings.isInAppFriendRequest() || settings.isPushFriendRequestCategory() || settings.isPushFriendRequest() || settings.isEmailFriendRequest())
                .boxInviteEnabled(settings.isInAppBoxInvite() || settings.isPushBoxInvite() || settings.isEmailBoxInvite())
                .mentionEnabled(settings.isInAppMention() || settings.isPushMention() || settings.isEmailMention())
                // Bổ sung DND
                .dndEnabled(settings.getDndEnabled())
                .dndStartHour(settings.getDndStartHour())
                .dndEndHour(settings.getDndEndHour())
                .build();
    }
}