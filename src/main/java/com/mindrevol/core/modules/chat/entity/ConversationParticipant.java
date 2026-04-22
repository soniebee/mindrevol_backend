// File: src/main/java/com/mindrevol/backend/modules/chat/entity/ConversationParticipant.java
package com.mindrevol.core.modules.chat.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_participants", indexes = {
    @Index(name = "idx_conv_user", columnList = "conversation_id, user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConversationParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_msg_id")
    private String lastReadMessageId; 

    @Column(name = "joined_at")
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Column(name = "role")
    @Builder.Default
    private String role = "MEMBER"; 

    // [CẬP NHẬT] Thêm 3 trường cho tính năng cá nhân hóa
    @Column(name = "is_pinned")
    @Builder.Default
    private boolean isPinned = false;

    @Column(name = "is_muted")
    @Builder.Default
    private boolean isMuted = false;

    @Column(name = "is_hidden")
    @Builder.Default
    private boolean isHidden = false;
}