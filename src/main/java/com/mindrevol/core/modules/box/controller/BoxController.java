package com.mindrevol.core.modules.box.controller;

// 1. Đảm bảo có đầy đủ các import này
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boxes")
@RequiredArgsConstructor
public class BoxController {

    // 2. Phải có dòng khai báo này thì các hàm bên dưới mới dùng được 'boxService'
    private final BoxService boxService;

    // 1. Tạo Box mới
    @PostMapping
    public ResponseEntity<BoxDetailResponse> createBox(
            @Valid @RequestBody CreateBoxRequest request,
            @RequestHeader("X-User-Id") String userId) {

        BoxDetailResponse response = boxService.createBox(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Lấy danh sách Box
    @GetMapping
    public ResponseEntity<Page<BoxResponse>> getMyBoxes(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivityAt").descending());
        Page<BoxResponse> responses = boxService.getMyBoxes(userId, pageable);
        return ResponseEntity.ok(responses);
    }

    // 3. Lấy chi tiết Box
    @GetMapping("/{boxId}")
    public ResponseEntity<BoxDetailResponse> getBoxDetail(
            @PathVariable String boxId,
            @RequestHeader("X-User-Id") String userId) {

        BoxDetailResponse response = boxService.getBoxDetail(boxId, userId);
        return ResponseEntity.ok(response);
    }

    // 4. Cập nhật thông tin Box
    @PutMapping("/{boxId}")
    public ResponseEntity<BoxDetailResponse> updateBox(
            @PathVariable String boxId,
            @RequestBody UpdateBoxRequest request,
            @RequestHeader("X-User-Id") String userId) {

        BoxDetailResponse response = boxService.updateBox(boxId, request, userId);
        return ResponseEntity.ok(response);
    }

    // 5. Xóa Box
    @DeleteMapping("/{boxId}")
    public ResponseEntity<Void> deleteBox(
            @PathVariable String boxId,
            @RequestHeader("X-User-Id") String userId) {

        boxService.deleteBox(boxId, userId);
        return ResponseEntity.noContent().build();
    }

    // 6. Rời khỏi Box
    @DeleteMapping("/{boxId}/leave")
    public ResponseEntity<Void> leaveBox(
            @PathVariable String boxId,
            @RequestHeader("X-User-Id") String userId) {

        boxService.leaveBox(boxId, userId);
        return ResponseEntity.noContent().build();
    }
    // 7. Gửi lời mời tham gia Box
    @PostMapping("/{boxId}/invites")
    public ResponseEntity<Void> inviteMember(
            @PathVariable String boxId,
            @Valid @RequestBody InviteMemberRequest request,
            @RequestHeader("X-User-Id") String inviterId) {
        boxService.inviteMember(boxId, request.getInviteeId(), inviterId);
        return ResponseEntity.ok().build();
    }

    // 8. Chấp nhận hoặc Từ chối lời mời
    @PostMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> handleInvitation(
            @PathVariable String invitationId,
            @RequestParam boolean accept,
            @RequestHeader("X-User-Id") String userId) {
        boxService.handleInvitation(invitationId, accept, userId);
        return ResponseEntity.ok().build();
    }

    // 9. Kích thành viên khỏi Box (Đuổi)
    @DeleteMapping("/{boxId}/members/{memberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable String boxId,
            @PathVariable String memberId,
            @RequestHeader("X-User-Id") String adminId) {
        boxService.kickMember(boxId, memberId, adminId);
        return ResponseEntity.noContent().build();
    }

    // 10. Đổi quyền thành viên (Ví dụ: Cấp quyền ADMIN)
    @PutMapping("/{boxId}/members/{memberId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable String boxId,
            @PathVariable String memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader("X-User-Id") String adminId) {
        boxService.updateMemberRole(boxId, memberId, request.getRole(), adminId);
        return ResponseEntity.ok().build();
    }
}