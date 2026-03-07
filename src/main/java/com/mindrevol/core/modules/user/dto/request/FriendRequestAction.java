package com.mindrevol.core.modules.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendRequestAction {
    @NotNull(message = "Target User ID is required")
    private String targetUserId; // [UUID] String
}