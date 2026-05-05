package com.mindrevol.core.modules.call.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSession implements Serializable {
    private String roomId;
    private String conversationId;
    private String callerId;
    private String receiverId; // Nếu là gọi nhóm, đây sẽ là boxId
    private String callerName;
    private String callerAvatar;
    private String callType; // VIDEO hoặc VOICE
    private String status;
    private long timestamp;
    private long startTime;
    
    // 🔥 THÊM TRƯỜNG NÀY CHO GỌI NHÓM
    @Builder.Default
    private boolean isGroup = false; 
}