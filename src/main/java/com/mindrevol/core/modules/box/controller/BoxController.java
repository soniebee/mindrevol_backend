package com.mindrevol.core.modules.box.controller;

// Các import dùng chung của dự án
import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;

// Các import của module Box
import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.InviteMemberRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateMemberRoleRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.service.BoxService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boxes")
@RequiredArgsConstructor
public class BoxController {

    // Phải có dòng khai báo này thì các hàm bên dưới mới dùng được 'boxService'
    private final BoxService boxService;

    // 1. Tạo Box mới
    @PostMapping
    public ApiResponse<BoxDetailResponse> createBox(@Valid @RequestBody CreateBoxRequest request) {
        String userId = SecurityUtils.getCurrentUserId(); // Tự động lấy ID từ token an toàn
        return ApiResponse.success(boxService.createBox(request, userId));
    }

    // 2. Lấy danh sách Box
    @GetMapping
    public ApiResponse<Page<BoxResponse>> getMyBoxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivityAt").descending());
        return ApiResponse.success(boxService.getMyBoxes(userId, pageable));
    }

    // 3. Lấy chi tiết Box
    @GetMapping("/{boxId}")
    public ApiResponse<BoxDetailResponse> getBoxDetail(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(boxService.getBoxDetail(boxId, userId));
    }

    // 4. Cập nhật thông tin Box
    @PutMapping("/{boxId}")
    public ApiResponse<BoxDetailResponse> updateBox(
            @PathVariable String boxId,
            @RequestBody UpdateBoxRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(boxService.updateBox(boxId, request, userId));
    }

    // 5. Xóa Box
    @DeleteMapping("/{boxId}")
    public ApiResponse<Void> deleteBox(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        boxService.deleteBox(boxId, userId);
        return ApiResponse.success(null);
    }

    // 6. Rời khỏi Box
    @DeleteMapping("/{boxId}/leave")
    public ApiResponse<Void> leaveBox(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        boxService.leaveBox(boxId, userId);
        return ApiResponse.success(null);
    }

    // 7. Gửi lời mời tham gia Box
    @PostMapping("/{boxId}/invites")
    public ApiResponse<Void> inviteMember(
            @PathVariable String boxId,
            @Valid @RequestBody InviteMemberRequest request) {

        String inviterId = SecurityUtils.getCurrentUserId();
        boxService.inviteMember(boxId, request.getInviteeId(), inviterId);
        return ApiResponse.success(null);
    }

    // 8. Chấp nhận hoặc Từ chối lời mời
    @PostMapping("/invitations/{invitationId}")
    public ApiResponse<Void> handleInvitation(
            @PathVariable String invitationId,
            @RequestParam boolean accept) {

        String userId = SecurityUtils.getCurrentUserId();
        boxService.handleInvitation(invitationId, accept, userId);
        return ApiResponse.success(null);
    }

    // 9. Kích thành viên khỏi Box (Đuổi)
    @DeleteMapping("/{boxId}/members/{memberId}")
    public ApiResponse<Void> kickMember(
            @PathVariable String boxId,
            @PathVariable String memberId) {

        String adminId = SecurityUtils.getCurrentUserId();
        boxService.kickMember(boxId, memberId, adminId);
        return ApiResponse.success(null);
    }

    // 10. Đổi quyền thành viên (Ví dụ: Cấp quyền ADMIN)
    @PutMapping("/{boxId}/members/{memberId}/role")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable String boxId,
            @PathVariable String memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {

        String adminId = SecurityUtils.getCurrentUserId();
        boxService.updateMemberRole(boxId, memberId, request.getRole(), adminId);
        return ApiResponse.success(null);
    }

    // 11. (MỚI THÊM) Chuyển nhượng Chủ Phòng
    @PutMapping("/{boxId}/transfer-ownership/{newOwnerId}")
    public ApiResponse<Void> transferOwnership(
            @PathVariable String boxId,
            @PathVariable String newOwnerId) {

        String currentOwnerId = SecurityUtils.getCurrentUserId();
        boxService.transferOwnership(boxId, newOwnerId, currentOwnerId);
        return ApiResponse.success(null);
    }
}