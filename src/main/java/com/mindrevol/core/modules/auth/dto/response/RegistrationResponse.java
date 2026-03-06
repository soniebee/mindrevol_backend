package com.mindrevol.core.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về sau khi registration hoàn thành
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private String userId;              // ID của user mới tạo
    private String email;               // Email
    private String handle;              // Handle
    private String fullname;            // Tên đầy đủ
    private String accessToken;         // JWT Access Token để login ngay
    private String refreshToken;        // Refresh Token
    private String message;             // Thông báo thành công
}


