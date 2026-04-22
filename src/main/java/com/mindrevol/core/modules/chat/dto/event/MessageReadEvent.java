// File: src/main/java/com/mindrevol/backend/modules/chat/dto/event/MessageReadEvent.java
package com.mindrevol.core.modules.chat.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageReadEvent {
    private String conversationId;    
    private String lastReadMessageId; 
    private String readerId; // Đổi tên thành readerId cho chuẩn xác
}