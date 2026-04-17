// File: src/main/java/com/mindrevol/backend/modules/chat/repository/ConversationRepository.java
package com.mindrevol.core.modules.chat.repository;

import com.mindrevol.core.modules.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p1 " +
           "JOIN c.participants p2 " +
           "WHERE p1.user.id = :userId1 AND p2.user.id = :userId2 AND c.boxId IS NULL")
    List<Conversation> findByUsers(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // [CẬP NHẬT] Lọc isHidden = false và ORDER BY isPinned DESC lên đầu
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND p.isHidden = false " +
           "ORDER BY p.isPinned DESC, c.lastMessageAt DESC")
    List<Conversation> findValidConversationsByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM Conversation c WHERE c.boxId = :boxId")
    Optional<Conversation> findByBoxId(@Param("boxId") String boxId);
}