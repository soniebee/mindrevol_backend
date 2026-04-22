package com.mindrevol.core.modules.box.event;

import com.mindrevol.core.modules.box.entity.BoxRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxRoleUpdatedEvent {
    private String boxId;
    private String boxName;
    private String memberId;      // Người được thay đổi vai trò
    private BoxRole oldRole;       // Vai trò cũ
    private BoxRole newRole;       // Vai trò mới
    private String adminId;        // Người thực hiện hành động
}

