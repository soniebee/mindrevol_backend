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
    private String id;              // [SỬA] Long -> String
    private String clientSideId;
    private String conversationId;  // [SỬA] Long -> String
    
    private String senderId;        // [SỬA] Long -> String
    private String senderAvatar;
    
    private String content;
    private MessageType type;
    private Map<String, Object> metadata;
    
    private MessageDeliveryStatus deliveryStatus;
    private boolean isDeleted;
    private String replyToMsgId;    // [SỬA] Long -> String
    
    private LocalDateTime createdAt;
}