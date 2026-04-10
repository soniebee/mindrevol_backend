// File: src/main/java/com/mindrevol/backend/modules/chat/service/ChatService.java
package com.mindrevol.core.modules.chat.service;

import com.mindrevol.core.common.dto.CursorPageResponse;
import com.mindrevol.core.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.core.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.core.modules.chat.dto.response.MessageResponse;
import com.mindrevol.core.modules.chat.entity.Conversation;

import java.util.List;

public interface ChatService {
    MessageResponse sendMessage(String senderId, SendMessageRequest request);
    List<ConversationResponse> getUserConversations(String userId);
    
    CursorPageResponse<MessageResponse> getConversationMessages(String conversationId, String cursor, int limit);
    CursorPageResponse<MessageResponse> getMessagesWithUser(String currentUserId, String partnerId, String cursor, int limit);
    
    void markConversationAsRead(String conversationId, String userId);
    Conversation getConversationById(String id);
    ConversationResponse getOrCreateConversation(String senderId, String receiverId);
    Conversation createBoxConversation(String boxId, String boxName, String creatorId);
    void updateBoxConversationInfo(String boxId, String newName);
    void addUserToBoxConversation(String boxId, String userId);
    void removeUserFromBoxConversation(String boxId, String userId);
    ConversationResponse getBoxConversation(String boxId, String userId);

    void deleteMessage(String messageId, String userId);
    MessageResponse reactToMessage(String messageId, String userId, String reactionType);
    long getUnreadBadgeCount(String userId);
    MessageResponse editMessage(String messageId, String userId, String newContent);

    // [CẬP NHẬT] Thêm 3 hàm mới
    void togglePinConversation(String conversationId, String userId);
    void toggleMuteConversation(String conversationId, String userId);
    void hideConversation(String conversationId, String userId);
    
    MessageResponse togglePinMessage(String messageId, String userId);
    List<MessageResponse> getPinnedMessages(String conversationId);
    List<MessageResponse> searchMessages(String conversationId, String keyword);
    
 // Thêm vào: src/main/java/com/mindrevol/backend/modules/chat/service/ChatService.java
    CursorPageResponse<MessageResponse> jumpToMessage(String conversationId, String messageId, int limit);
}