// File: src/main/java/com/mindrevol/backend/modules/chat/controller/ChatController.java
package com.mindrevol.core.modules.chat.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.dto.CursorPageResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.core.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.core.modules.chat.dto.response.MessageResponse;
import com.mindrevol.core.modules.chat.service.ChatService;
import com.mindrevol.core.modules.chat.dto.request.WebRtcMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate; 

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(@RequestBody SendMessageRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(userId, request)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getUserConversations(userId)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<CursorPageResponse<MessageResponse>> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(chatService.getConversationMessages(conversationId, cursor, limit));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String conversationId) {
        String userId = SecurityUtils.getCurrentUserId();
        chatService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/conversations/init/{receiverId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @PathVariable String receiverId) {
        String senderId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getOrCreateConversation(senderId, receiverId)));
    }

    @GetMapping("/conversations/box/{boxId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getBoxConversation(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getBoxConversation(boxId, userId)));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable String messageId) {
        String userId = SecurityUtils.getCurrentUserId();
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/messages/{messageId}/react")
    public ResponseEntity<ApiResponse<MessageResponse>> reactToMessage(
            @PathVariable String messageId,
            @RequestParam String reactionType) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.reactToMessage(messageId, userId, reactionType)));
    }

    @GetMapping("/unread-badge")
    public ResponseEntity<ApiResponse<Long>> getUnreadBadge() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getUnreadBadgeCount(userId)));
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable String messageId,
            @RequestBody Map<String, String> requestBody) {
        String userId = SecurityUtils.getCurrentUserId();
        String newContent = requestBody.get("content");
        return ResponseEntity.ok(ApiResponse.success(chatService.editMessage(messageId, userId, newContent)));
    }

    // [CẬP NHẬT] Các API cho quản lý hội thoại
    @PutMapping("/conversations/{conversationId}/pin")
    public ResponseEntity<ApiResponse<Void>> togglePin(@PathVariable String conversationId) {
        chatService.togglePinConversation(conversationId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/conversations/{conversationId}/mute")
    public ResponseEntity<ApiResponse<Void>> toggleMute(@PathVariable String conversationId) {
        chatService.toggleMuteConversation(conversationId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/conversations/{conversationId}/hide")
    public ResponseEntity<ApiResponse<Void>> hideConversation(@PathVariable String conversationId) {
        chatService.hideConversation(conversationId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @MessageMapping("/chat/webrtc")
    public void handleWebRtcSignaling(@Payload WebRtcMessage message) {
        messagingTemplate.convertAndSend(
            "/topic/webrtc." + message.getTargetId(),
            message
        );
    }
    
    
    @PutMapping("/messages/{messageId}/pin")
    public ResponseEntity<ApiResponse<MessageResponse>> togglePinMessage(@PathVariable String messageId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.togglePinMessage(messageId, SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/conversations/{conversationId}/pinned")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPinnedMessages(@PathVariable String conversationId) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getPinnedMessages(conversationId)));
    }

    @GetMapping("/conversations/{conversationId}/search")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> searchMessages(
            @PathVariable String conversationId, @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(chatService.searchMessages(conversationId, keyword)));
    }
    
// Thêm vào: src/main/java/com/mindrevol/backend/modules/chat/controller/ChatController.java
    
    @GetMapping("/conversations/{conversationId}/messages/jump")
    public ResponseEntity<ApiResponse<CursorPageResponse<MessageResponse>>> jumpToMessage(
            @PathVariable String conversationId,
            @RequestParam String messageId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(chatService.jumpToMessage(conversationId, messageId, limit)));
    }
}