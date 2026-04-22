// File: src/main/java/com/mindrevol/backend/modules/chat/dto/request/TypingEvent.java
package com.mindrevol.core.modules.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingEvent {
    private String conversationId; 
    private String senderId;       
    private boolean isTyping; 
    // Bỏ receiverId vì event sẽ gửi thẳng vào topic của conversation
}