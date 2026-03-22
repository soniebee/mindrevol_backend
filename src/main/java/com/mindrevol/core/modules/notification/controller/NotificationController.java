package com.mindrevol.core.modules.notification.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyNotifications(userId, pageable)));
    }

    // [TASK-102] Logic là count unseen nhưng path giữ nguyên để tương thích Frontend cũ nếu cần
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> countUnseen() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnseen(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // [TASK-102] API Đánh dấu đã thấy (khi mở Panel)
    @PatchMapping("/seen-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsSeen() {
        String userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsSeen(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null, "Đã xóa thông báo");
    }

    @DeleteMapping("/all")
    public ApiResponse<Void> deleteAllMyNotifications() {
        notificationService.deleteAllMyNotifications(SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null, "Đã dọn sạch thông báo");
    }
}