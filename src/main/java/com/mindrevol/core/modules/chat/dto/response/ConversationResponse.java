// File: src/main/java/com/mindrevol/backend/modules/chat/dto/response/ConversationResponse.java
package com.mindrevol.core.modules.chat.dto.response;

import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private String id; 
    
    private UserSummaryResponse partner; 
    
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private String lastSenderId; 
    
    private long unreadCount;
    private String status;
    private String boxId;

    private String boxName;
    private String boxAvatar;

    // [CẬP NHẬT] Các cờ trạng thái cá nhân
    private boolean isPinned;
    private boolean isMuted;
}