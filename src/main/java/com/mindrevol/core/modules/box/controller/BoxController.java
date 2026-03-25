package com.mindrevol.core.modules.box.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.InviteMemberRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateMemberRoleRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.dto.response.BoxInvitationResponse;
import com.mindrevol.core.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.core.modules.box.service.BoxService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boxes")
@RequiredArgsConstructor
public class BoxController {

    private final BoxService boxService;

    // 1. Tạo Box mới
    @PostMapping
    public ApiResponse<BoxDetailResponse> createBox(@Valid @RequestBody CreateBoxRequest request) {
        return ApiResponse.success(boxService.createBox(request, SecurityUtils.getCurrentUserId()));
    }

    // 2. Lấy danh sách Box của tôi
    @GetMapping
    public ApiResponse<Page<BoxResponse>> getMyBoxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivityAt").descending());
        return ApiResponse.success(boxService.getMyBoxes(SecurityUtils.getCurrentUserId(), pageable));
    }

    // 3. Lấy chi tiết Box
    @GetMapping("/{boxId}")
    public ApiResponse<BoxDetailResponse> getBoxDetail(@PathVariable String boxId) {
        return ApiResponse.success(boxService.getBoxDetail(boxId, SecurityUtils.getCurrentUserId()));
    }

    // 4. Cập nhật thông tin Box
    @PutMapping("/{boxId}")
    public ApiResponse<BoxDetailResponse> updateBox(
            @PathVariable String boxId,
            @RequestBody UpdateBoxRequest request) {
        return ApiResponse.success(boxService.updateBox(boxId, request, SecurityUtils.getCurrentUserId()));
    }

    // 5. Xóa Box
    @DeleteMapping("/{boxId}")
    public ApiResponse<Void> deleteBox(@PathVariable String boxId) {
        boxService.deleteBox(boxId, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // 6. Rời khỏi Box
    @DeleteMapping("/{boxId}/leave")
    public ApiResponse<Void> leaveBox(@PathVariable String boxId) {
        boxService.leaveBox(boxId, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // 7. Gửi lời mời tham gia Box
    @PostMapping("/{boxId}/invites")
    public ApiResponse<Void> inviteMember(
            @PathVariable String boxId,
            @Valid @RequestBody InviteMemberRequest request) {
        boxService.inviteMember(boxId, request.getInviteeId(), SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // 8. Chấp nhận hoặc Từ chối lời mời
    @PostMapping("/invitations/{invitationId}")
    public ApiResponse<Void> handleInvitation(
            @PathVariable String invitationId,
            @RequestParam boolean accept) {
        boxService.handleInvitation(invitationId, accept, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // 9. Kích thành viên khỏi Box (Đuổi)
    @DeleteMapping("/{boxId}/members/{memberId}")
    public ApiResponse<Void> kickMember(
            @PathVariable String boxId,
            @PathVariable String memberId) {
        boxService.kickMember(boxId, memberId, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // 10. Đổi quyền thành viên (Ví dụ: Cấp quyền ADMIN)
    @PutMapping("/{boxId}/members/{memberId}/role")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable String boxId,
            @PathVariable String memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        boxService.updateMemberRole(boxId, memberId, request.getRole(), SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // 11. Chuyển nhượng Chủ Phòng
    @PutMapping("/{boxId}/transfer-ownership/{newOwnerId}")
    public ApiResponse<Void> transferOwnership(
            @PathVariable String boxId,
            @PathVariable String newOwnerId) {
        boxService.transferOwnership(boxId, newOwnerId, SecurityUtils.getCurrentUserId());
        return ApiResponse.success(null);
    }

    // =========================================================================
    // CÁC API PHỤC VỤ HIỂN THỊ DỮ LIỆU (ĐƯỢC PHỤC HỒI TỪ BẢN CŨ)
    // =========================================================================

    // 12. Lấy danh sách lời mời đang chờ của tôi
    @GetMapping("/invitations/me")
    public ApiResponse<List<BoxInvitationResponse>> getMyPendingInvitations() {
        return ApiResponse.success(boxService.getMyPendingInvitations(SecurityUtils.getCurrentUserId()));
    }

    // 13. Lấy danh sách thành viên trong Box
    @GetMapping("/{boxId}/members")
    public ApiResponse<Page<BoxMemberResponse>> getBoxMembers(
            @PathVariable String boxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(boxService.getBoxMembers(boxId, SecurityUtils.getCurrentUserId(), pageable));
    }

    // 14. Lấy danh sách hành trình trong Box
    @GetMapping("/{boxId}/journeys")
    public ApiResponse<Page<JourneyResponse>> getBoxJourneys(
            @PathVariable String boxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(boxService.getBoxJourneys(boxId, SecurityUtils.getCurrentUserId(), pageable));
    }
}