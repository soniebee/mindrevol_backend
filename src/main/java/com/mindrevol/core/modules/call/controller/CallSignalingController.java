package com.mindrevol.core.modules.call.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.call.dto.CallSession;
import com.mindrevol.core.modules.call.service.CallSignalingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calls/signaling")
@RequiredArgsConstructor
public class CallSignalingController {
    private final CallSignalingService signalingService;

    @PostMapping("/initiate")
    public ApiResponse<CallSession> initiateCall(
            @RequestParam String receiverId, 
            @RequestParam String type,
            @RequestParam String conversationId) { // <--- NHẬN TỪ FRONTEND
        return ApiResponse.success(signalingService.initiateCall(
            SecurityUtils.getCurrentUserId(), receiverId, type, conversationId));
    }

    @PostMapping("/respond/{roomId}")
    public ApiResponse<Void> respondCall(@PathVariable String roomId, @RequestParam String action) {
        signalingService.respondToCall(roomId, SecurityUtils.getCurrentUserId(), action);
        return ApiResponse.success(null);
    }

    @PostMapping("/end/{roomId}")
    public ApiResponse<Void> endCall(@PathVariable String roomId) {
        signalingService.endCall(roomId);
        return ApiResponse.success(null);
    }
    
    @GetMapping("/flush-ghost")
    public ApiResponse<Void> flushGhostCalls() {
        signalingService.cleanupStaleCalls();
        return ApiResponse.success(null, "Đã quét và xóa ghost call ngay lập tức!");
    }
}