package com.mindrevol.core.modules.box.service;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.entity.BoxRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoxService {
    BoxDetailResponse createBox(CreateBoxRequest request, String userId);
    Page<BoxResponse> getMyBoxes(String userId, Pageable pageable);
    BoxDetailResponse getBoxDetail(String boxId, String userId);
    BoxDetailResponse updateBox(String boxId, UpdateBoxRequest request, String userId);
    void deleteBox(String boxId, String userId);
    void leaveBox(String boxId, String userId);
    void inviteMember(String boxId, String inviteeId, String inviterId);
    void handleInvitation(String invitationId, boolean isAccepted, String userId);
    void kickMember(String boxId, String memberId, String adminId);
    void updateMemberRole(String boxId, String memberId, BoxRole newRole, String adminId);

    // 🔥 Tính năng mới siêu xịn: Chuyển nhượng quyền chủ phòng
    void transferOwnership(String boxId, String newOwnerId, String currentOwnerId);
}