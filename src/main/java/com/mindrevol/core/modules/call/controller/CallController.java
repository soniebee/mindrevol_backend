package com.mindrevol.core.modules.call.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.call.service.strategy.CallStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
public class CallController {

    // Tạm thời hardcode gọi thẳng Zego. Nếu sau này có nhiều Strategy, 
    // bạn dùng Map<String, CallStrategy> giống như bên Payment.
    private final CallStrategy zegoCallStrategyImpl; 

    @GetMapping("/token/{roomId}")
    public ApiResponse<String> getCallToken(@PathVariable String roomId) {
        String userId = SecurityUtils.getCurrentUserId();
        // Lấy Token từ Strategy
        String token = zegoCallStrategyImpl.generateCallToken(userId, roomId);
        return ApiResponse.success(token);
    }
}