// File: src/main/java/com/mindrevol/backend/modules/chat/repository/MessageReactionRepository.java (TẠO MỚI)
package com.mindrevol.core.modules.chat.repository;

import com.mindrevol.core.modules.chat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, String> {
    Optional<MessageReaction> findByMessageIdAndUserId(String messageId, String userId);
}