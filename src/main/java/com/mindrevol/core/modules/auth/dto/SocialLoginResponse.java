package com.mindrevol.core.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về sau khi Social Login thành công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginResponse {

    private String userId;          // ID user (tạo mới hoặc existing)
    private String email;
    private String name;
    private String handle;          // Handle (có thể generate nếu user mới)
    private String avatarUrl;
    private String accessToken;     // JWT token để login
    private String refreshToken;
    private Boolean isNewUser;      // true nếu user mới tạo
    private String message;
}

