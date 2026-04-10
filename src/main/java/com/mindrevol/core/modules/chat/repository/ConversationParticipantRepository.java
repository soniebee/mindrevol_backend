// File: src/main/java/com/mindrevol/backend/modules/chat/repository/ConversationParticipantRepository.java (TẠO MỚI)
package com.mindrevol.core.modules.chat.repository;

import com.mindrevol.core.modules.chat.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, String> {
    Optional<ConversationParticipant> findByConversationIdAndUserId(String conversationId, String userId);
    List<ConversationParticipant> findByConversationId(String conversationId);
}