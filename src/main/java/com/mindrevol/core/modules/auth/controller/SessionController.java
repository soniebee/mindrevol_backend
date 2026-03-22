package com.mindrevol.core.modules.auth.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.auth.dto.request.LogoutRequest;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionsGroupedResponse;
import com.mindrevol.core.modules.auth.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Session", description = "Session and device management")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token", description = "Issue a new access token from refresh token.")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;
        JwtResponse jwtResponse = sessionService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke current refresh token.")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        sessionService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully."));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Device list", description = "View active login sessions.")
    public ResponseEntity<ApiResponse<UserSessionsGroupedResponse>> getSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String currentToken,
            HttpServletRequest request) {
        UserSessionsGroupedResponse sessions = sessionService.getAllSessions(userDetails.getUsername(), currentToken, request);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Logout device", description = "Revoke a specific login session.")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        sessionService.revokeSession(sessionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Device logged out successfully."));
    }
}
