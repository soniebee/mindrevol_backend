package com.mindrevol.core.modules.box.service;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.entity.BoxRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoxService {
    // 1. Tạo Box
    BoxDetailResponse createBox(CreateBoxRequest request, String userId);

    // 2. Lấy danh sách Box
    Page<BoxResponse> getMyBoxes(String userId, Pageable pageable);

    // 3. Lấy chi tiết Box
    BoxDetailResponse getBoxDetail(String boxId, String userId);

    // 4. Cập nhật thông tin Box (Chỉ Admin)
    BoxDetailResponse updateBox(String boxId, UpdateBoxRequest request, String userId);

    // 5. Xóa Box (Chỉ Admin)
    void deleteBox(String boxId, String userId);

    // 6. Rời khỏi Box (Thành viên tự rời)
    void leaveBox(String boxId, String userId);

    // 7. Mời thành viên vào Box
    void inviteMember(String boxId, String inviteeId, String inviterId);

    // 8. Xử lý lời mời (Đồng ý / Từ chối)
    void handleInvitation(String invitationId, boolean isAccepted, String userId);

    // 9. Đuổi thành viên (Chỉ Admin)
    void kickMember(String boxId, String memberId, String adminId);

    // 10. Cập nhật quyền thành viên (Chỉ Admin/Owner)
    void updateMemberRole(String boxId, String memberId, BoxRole newRole, String adminId);
}