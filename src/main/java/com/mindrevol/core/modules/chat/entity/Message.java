// File: src/main/java/com/mindrevol/backend/modules/chat/entity/Message.java
package com.mindrevol.core.modules.chat.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Message extends BaseEntity {

    @Column(name = "client_side_id") 
    private String clientSideId; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // [ĐÃ XÓA] receiver_id vì tin nhắn nhóm không gửi cho cá nhân cụ thể
    // Nếu muốn lưu người nhận cụ thể (DM), hệ thống dựa vào ConversationParticipant

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type; 

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    @Builder.Default
    private MessageDeliveryStatus deliveryStatus = MessageDeliveryStatus.SENT;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "reply_to_msg_id")
    private String replyToMsgId;

    // [THÊM MỚI] Liên kết Reactions
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MessageReaction> reactions = new ArrayList<>();
    
    @Column(name = "is_pinned") @Builder.Default 
    private boolean isPinned = false;
}