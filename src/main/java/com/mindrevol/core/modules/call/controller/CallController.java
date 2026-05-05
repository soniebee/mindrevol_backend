package com.mindrevol.core.modules.call.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.call.dto.CallSession;
import com.mindrevol.core.modules.call.service.CallSignalingService;
import com.mindrevol.core.modules.call.service.strategy.CallStrategy; 
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallSignalingService callSignalingService;
    private final CallStrategy callStrategy; 

    @PostMapping("/signaling/initiate")
    public ApiResponse<CallSession> initiateCall(@RequestParam String receiverId, @RequestParam String type, @RequestParam String conversationId) {
        return ApiResponse.success(callSignalingService.initiateCall(SecurityUtils.getCurrentUserId(), receiverId, type, conversationId));
    }

    // API GỌI NHÓM TRONG BOX CHAT
    @PostMapping("/signaling/initiate-box")
    public ApiResponse<CallSession> initiateBoxCall(@RequestParam String boxId, @RequestParam String type, @RequestParam String conversationId) {
        return ApiResponse.success(callSignalingService.initiateBoxCall(SecurityUtils.getCurrentUserId(), boxId, conversationId, type));
    }

    @PostMapping("/signaling/respond/{roomId}")
    public ApiResponse<Void> respondToCall(@PathVariable String roomId, @RequestParam String action) {
        callSignalingService.respondToCall(roomId, SecurityUtils.getCurrentUserId(), action);
        return ApiResponse.success(null);
    }

    @PostMapping("/signaling/end/{roomId}")
    public ApiResponse<Void> endCall(@PathVariable String roomId) {
        callSignalingService.endCall(roomId);
        return ApiResponse.success(null);
    }

    // API RỜI/KẾT THÚC GỌI NHÓM
    @PostMapping("/signaling/end-box/{roomId}")
    public ApiResponse<Void> endBoxCall(@PathVariable String roomId, @RequestParam String boxId) {
        callSignalingService.leaveBoxCall(boxId, roomId);
        return ApiResponse.success(null);
    }

    // API LẤY TOKEN CỦA ZEGO CLOUD
    @GetMapping("/token/{roomId}")
    public ApiResponse<String> getCallToken(@PathVariable String roomId) {
        String token = callStrategy.generateCallToken(SecurityUtils.getCurrentUserId(), roomId);
        return ApiResponse.success(token);
    }

    @GetMapping("/signaling/flush-ghost")
    public ApiResponse<Void> flushGhostCalls() {
        callSignalingService.cleanupStaleCalls();
        return ApiResponse.success(null, "Đã dọn dẹp ghost call ngay lập tức!");
    }
}