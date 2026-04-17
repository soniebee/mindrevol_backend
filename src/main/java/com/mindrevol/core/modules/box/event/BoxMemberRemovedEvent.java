package com.mindrevol.core.modules.box.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxMemberRemovedEvent {
    private String boxId;
    private String boxName;
    private String removedUserId; // Người bị đuổi ra khỏi Box
    private String adminId;       // Người thực hiện hành động (Admin/Owner)
}

