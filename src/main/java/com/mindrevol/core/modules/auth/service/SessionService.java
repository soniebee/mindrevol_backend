package com.mindrevol.core.modules.auth.service;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionsGroupedResponse;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
public interface SessionService {
    // Create token and persist a new login session.
    default JwtResponse createTokenAndSession(User user, HttpServletRequest request) {
        return createTokenAndSession(user, request, null);
    }
    JwtResponse createTokenAndSession(User user, HttpServletRequest request, String deviceId);
    JwtResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    UserSessionsGroupedResponse getAllSessions(String userEmail, String currentTokenRaw, HttpServletRequest request);
    void revokeSession(String sessionId, String userEmail);
}
