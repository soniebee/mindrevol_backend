package com.mindrevol.core.modules.call.job;

import com.mindrevol.core.modules.call.service.CallSignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallCleanupJob {

    private final CallSignalingService callSignalingService;

    // Chạy ngầm định kỳ mỗi 5 phút một lần
    @Scheduled(fixedRate = 300000)
    public void cleanupGhostCalls() {
        try {
            callSignalingService.cleanupStaleCalls();
        } catch (Exception e) {
            log.error("Lỗi khi chạy Job dọn rác cuộc gọi ma: {}", e.getMessage());
        }
    }
}