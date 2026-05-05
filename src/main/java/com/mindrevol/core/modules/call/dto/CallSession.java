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
    private String conversationId; // <--- THÊM MỚI
    private String callerId;
    private String callerName;
    private String callerAvatar;
    private String receiverId;
    private String callType;
    private String status;
    private long timestamp;
    private long startTime;        // <--- THÊM MỚI
}