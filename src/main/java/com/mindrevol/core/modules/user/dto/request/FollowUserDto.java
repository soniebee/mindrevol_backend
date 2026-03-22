package com.mindrevol.core.modules.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để theo dõi người dùng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUserDto {
    private String targetUserId;
}

