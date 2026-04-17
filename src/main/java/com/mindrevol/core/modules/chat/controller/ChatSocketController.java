package com.mindrevol.core.modules.chat.controller;

import com.mindrevol.core.modules.chat.dto.request.TypingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Nhận sự kiện Typing từ Client A gửi lên
     * Client gửi tới: /app/chat/typing
     */
    @MessageMapping("/chat/typing")
    public void handleTypingEvent(@Payload TypingEvent event) {
        // [THÊM BẢO VỆ] Tránh lỗi NullPointerException khi chat Box (nhóm) không có receiverId cụ thể
        if (event.getReceiverId() != null && !event.getReceiverId().isEmpty() && !event.getReceiverId().equals("null")) {
            messagingTemplate.convertAndSendToUser(
                event.getReceiverId(),
                "/queue/typing",
                event
            );
        }
}
}