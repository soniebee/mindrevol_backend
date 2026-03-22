package com.mindrevol.core.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Response cho thông tin người dùng công khai (rút gọn)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicResponse {
    private String id;
    private String handle;
    private String fullname;
    private String bio;
    private String avatarUrl;
    private String website;
    private boolean isOnline;
    private String friendshipStatus; // FRIEND, FOLLOW, BLOCKED, NONE
}

