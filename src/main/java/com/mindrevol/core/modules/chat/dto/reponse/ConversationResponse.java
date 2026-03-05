package com.mindrevol.core.modules.chat.dto.reponse;

import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private String id; // [SỬA] Long -> String
    
    private UserSummaryResponse partner; 
    
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private String lastSenderId; // [SỬA] Long -> String
    
    private long unreadCount;
    private String status;
}