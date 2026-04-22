package com.mindrevol.core.modules.box.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxInvitedEvent {
    private String invitationId;
    private String boxId;
    private String boxName;
    private String senderId;    // Người gửi lời mời
    private String recipientId; // Người nhận lời mời (người sẽ nhận được thông báo)
}