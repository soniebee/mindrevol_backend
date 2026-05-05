package com.mindrevol.core.modules.call.listener;

import com.mindrevol.core.modules.call.service.CallSignalingService;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallDisconnectListener {

    private final CallSignalingService callSignalingService;
    private final UserRepository userRepository;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            try {
                // Principal Name thường chứa Email (Do cấu hình JWT của bạn)
                userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                    log.info("Client rớt WebSocket (F5/Đóng tab), xử lý hủy cuộc gọi kẹt cho user: {}", user.getEmail());
                    callSignalingService.forceEndActiveCallsByUserId(user.getId());
                });
            } catch (Exception e) {
                log.error("Lỗi khi dọn dẹp ghost call: {}", e.getMessage());
            }
        }
    }
}