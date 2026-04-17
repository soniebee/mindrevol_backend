package com.mindrevol.core.modules.box.service;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxInvitationResponse;
import com.mindrevol.core.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.entity.BoxRole;
import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoxService {
    // 1. Tạo Box
    BoxDetailResponse createBox(CreateBoxRequest request, String userId);

    // 2. Lấy danh sách Box (Đã cập nhật thêm param)
    Page<BoxResponse> getMyBoxes(String userId, String tab, String search, Pageable pageable);

    // 3. Lấy chi tiết Box
    BoxDetailResponse getBoxDetail(String boxId, String userId);

    // 4. Cập nhật thông tin Box
    BoxDetailResponse updateBox(String boxId, UpdateBoxRequest request, String userId);

    // 5. Xóa Box
    void deleteBox(String boxId, String userId);

    // 6. Rời khỏi Box
    void leaveBox(String boxId, String userId);

    // 7. Mời thành viên
    void inviteMember(String boxId, String inviteeId, String inviterId);

    // 8. Xử lý lời mời
    void handleInvitation(String invitationId, boolean isAccepted, String userId);

    // 9. Đuổi thành viên
    void kickMember(String boxId, String memberId, String adminId);

    // 10. Cập nhật quyền
    void updateMemberRole(String boxId, String memberId, BoxRole newRole, String adminId);

    // 11. Chuyển nhượng
    void transferOwnership(String boxId, String newOwnerId, String currentOwnerId);

    // Đã cập nhật thêm param search
    List<BoxInvitationResponse> getMyPendingInvitations(String userId, String search);

    Page<BoxMemberResponse> getBoxMembers(String boxId, String userId, Pageable pageable);

    Page<JourneyResponse> getBoxJourneys(String boxId, String userId, Pageable pageable);
}