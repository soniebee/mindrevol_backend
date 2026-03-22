package com.mindrevol.core.modules.user.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.core.modules.user.service.UserBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Block", description = "User block management")
public class UserBlockController {

    private final UserBlockService userBlockService;

    // [UUID] @PathVariable String
    @PostMapping("/blocks/{targetId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Block user")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable String targetId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userBlockService.blockUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/blocks/{targetId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unblock user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable String targetId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userBlockService.unblockUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me/blocks")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get block list")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getBlockList() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userBlockService.getBlockList(userId)));
    }
}