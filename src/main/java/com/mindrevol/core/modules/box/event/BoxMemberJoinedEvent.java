package com.mindrevol.core.modules.box.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxMemberJoinedEvent {
    private String boxId;
    private String boxName;
    private String joinedUserId; // Người vừa mới gia nhập
}