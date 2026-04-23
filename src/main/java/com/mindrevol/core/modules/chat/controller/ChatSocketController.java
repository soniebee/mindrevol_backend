// File: src/main/java/com/mindrevol/core/modules/chat/controller/ChatSocketController.java
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

    @MessageMapping("/chat/typing")
    public void handleTypingEvent(@Payload TypingEvent event) {
        // [THAY ĐỔI QUAN TRỌNG] Bắn sự kiện Typing vào Topic của phòng thay vì gửi thẳng cho 1 cá nhân
        // Client sẽ subscribe: /topic/chat.{conversationId}.typing
        messagingTemplate.convertAndSend(
            "/topic/chat." + event.getConversationId() + ".typing",
            event
        );
    }
}