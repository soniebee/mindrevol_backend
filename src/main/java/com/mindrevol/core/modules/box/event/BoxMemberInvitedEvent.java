package com.mindrevol.core.modules.box.event;

import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoxMemberInvitedEvent {
    private final Box box;
    private final User inviter;
    private final User invitee;
}
