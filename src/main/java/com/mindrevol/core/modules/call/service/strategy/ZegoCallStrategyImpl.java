package com.mindrevol.core.modules.call.service.strategy;

import com.mindrevol.core.modules.call.utils.ZegoTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZegoCallStrategyImpl implements CallStrategy {

    @Value("${zego.app-id}")
    private long appId;

    @Value("${zego.server-secret}")
    private String serverSecret;

    @Override
    public String getProviderName() {
        return "ZEGOCLOUD";
    }

    @Override
    public String generateCallToken(String userId, String roomId) {
        // Token có hiệu lực 2 tiếng (7200 giây)
        return ZegoTokenUtils.generateToken04(appId, userId, serverSecret, 7200);
    }
}