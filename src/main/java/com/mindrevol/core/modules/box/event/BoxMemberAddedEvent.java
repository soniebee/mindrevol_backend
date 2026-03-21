package com.mindrevol.core.modules.box.event;

import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoxMemberAddedEvent {
    private final Box box;
    private final User adder;      // Người mời
    private final User newMember;  // Người được mời
}
