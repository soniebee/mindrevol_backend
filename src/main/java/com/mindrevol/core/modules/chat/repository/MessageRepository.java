// File: src/main/java/com/mindrevol/backend/modules/chat/repository/MessageRepository.java (CẬP NHẬT)
package com.mindrevol.core.modules.chat.repository;

import com.mindrevol.core.modules.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    // [THAY ĐỔI] Phân trang bằng Cursor (Lấy tin nhắn có ID nhỏ hơn (cũ hơn) Cursor hiện tại)
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.id < :cursor ORDER BY m.id DESC")
    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(
        @Param("conversationId") String conversationId, 
        @Param("cursor") String cursor, 
        Pageable pageable
    );

    // [THAY ĐỔI] Lấy trang đầu tiên (khi không truyền cursor)
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.id DESC")
    List<Message> findByConversationIdOrderByIdDesc(
        @Param("conversationId") String conversationId, 
        Pageable pageable
    );

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);

    // [THAY ĐỔI] Đếm tin nhắn chưa đọc dựa vào trạng thái của từng participant
    @Query("SELECT COUNT(m) FROM Message m " +
           "JOIN m.conversation c " +
           "JOIN c.participants p " +
           "WHERE c.id = :convId AND p.user.id = :userId " +
           "AND m.sender.id <> :userId " +
           "AND (p.lastReadMessageId IS NULL OR m.id > p.lastReadMessageId)")
    long countUnreadMessages(@Param("convId") String convId, @Param("userId") String userId);
    
 // Lấy các tin nhắn đã ghim trong hội thoại
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.isPinned = true AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<Message> findPinnedMessages(@Param("convId") String convId);

    // Tìm kiếm tin nhắn theo từ khóa (Không phân biệt hoa thường)
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.isDeleted = false AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchMessages(@Param("convId") String convId, @Param("keyword") String keyword);
    
// Thêm vào: src/main/java/com/mindrevol/backend/modules/chat/repository/MessageRepository.java
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.createdAt <= (SELECT m2.createdAt FROM Message m2 WHERE m2.id = :messageId) ORDER BY m.createdAt DESC")
    List<Message> findMessagesForJump(@Param("convId") String convId, @Param("messageId") String messageId, Pageable pageable);
}