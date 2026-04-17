// File: src/main/java/com/mindrevol/backend/modules/chat/entity/MessageReaction.java (TẠO MỚI)
package com.mindrevol.core.modules.chat.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import com.mindrevol.core.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "message_reactions", indexes = {
    @Index(name = "idx_msg_user", columnList = "message_id, user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MessageReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reaction_type", nullable = false)
    private String reactionType; // VD: "LIKE", "HEART", "HAHA"
}