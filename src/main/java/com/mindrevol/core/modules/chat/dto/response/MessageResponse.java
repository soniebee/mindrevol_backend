// File: src/main/java/com/mindrevol/backend/modules/chat/dto/response/MessageResponse.java
package com.mindrevol.core.modules.chat.dto.response;

import com.mindrevol.core.modules.chat.entity.MessageDeliveryStatus;
import com.mindrevol.core.modules.chat.entity.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class MessageResponse {
    private String id;              
    private String clientSideId;
    private String conversationId;  
    
    private String senderId;        
    private String senderAvatar;
    private String senderName; // Bổ sung cho chat nhóm
    
    private String content;
    private MessageType type;
    private Map<String, Object> metadata;
    
    private MessageDeliveryStatus deliveryStatus;
    private boolean isDeleted;
    private String replyToMsgId;    
    
    // [THÊM MỚI] Map UserId -> ReactionType
    private Map<String, String> reactions;
    
    private LocalDateTime createdAt;
    private boolean isPinned;
}