package com.mindrevol.core.modules.auth.service;

import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionsGroupedResponse;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface SessionService {
    
    // Tạo token và lưu session mới (Dùng chung cho Login & Register)
    JwtResponse createTokenAndSession(User user, HttpServletRequest request);

    // Làm mới token khi hết hạn
    JwtResponse refreshToken(String refreshToken);

    // Đăng xuất (Xóa session)
    void logout(String refreshToken);

    // Lấy danh sách các thiết bị đang đăng nhập
    UserSessionsGroupedResponse getAllSessions(String userEmail, String currentTokenRaw, HttpServletRequest request);

    // Đăng xuất từ xa một thiết bị cụ thể
    void revokeSession(String sessionId, String userEmail);
}