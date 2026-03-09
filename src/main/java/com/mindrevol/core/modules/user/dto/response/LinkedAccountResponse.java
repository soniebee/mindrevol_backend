package com.mindrevol.core.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Response cho thông tin kết nối tài khoản xã hội
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedAccountResponse {
    private String provider; // GOOGLE, FACEBOOK, APPLE, etc.
    private String email;
    private String avatarUrl;
    private boolean connected;
}

