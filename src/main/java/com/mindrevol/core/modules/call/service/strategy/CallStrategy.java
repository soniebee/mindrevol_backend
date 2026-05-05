package com.mindrevol.core.modules.call.service.strategy;

public interface CallStrategy {
    String getProviderName(); // VD: ZEGO, AGORA, CUSTOM_WEBRTC
    
    // Trả về Token để Frontend có quyền join vào phòng gọi
    String generateCallToken(String userId, String roomId);
}