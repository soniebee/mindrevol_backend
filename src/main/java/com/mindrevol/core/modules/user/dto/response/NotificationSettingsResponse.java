package com.mindrevol.core.modules.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSettingsResponse {
    private boolean commentEnabled;
    private boolean reactionEnabled;
    private boolean messageEnabled;
    private boolean journeyEnabled;
    private boolean friendRequestEnabled;
    private boolean boxInviteEnabled;
    private boolean mentionEnabled;
}

