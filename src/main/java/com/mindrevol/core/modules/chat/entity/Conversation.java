// File: src/main/java/com/mindrevol/backend/modules/chat/entity/Conversation.java
package com.mindrevol.core.modules.chat.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_last_message_at", columnList = "last_message_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Conversation extends BaseEntity {

    // ĐÃ XÓA user1 và user2. Thay bằng danh sách participants.
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConversationParticipant> participants = new ArrayList<>();

    @Column(name = "last_message_content")
    private String lastMessageContent; 

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_sender_id")
    private String lastSenderId; 

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "box_id")
    private String boxId;
}