package com.mindrevol.core.modules.chat.service.impl;

import com.mindrevol.core.common.dto.CursorPageResponse;
import com.mindrevol.core.modules.chat.entity.Conversation;
import com.mindrevol.core.modules.chat.entity.ConversationParticipant;
import com.mindrevol.core.modules.chat.entity.ConversationStatus;
import com.mindrevol.core.modules.chat.entity.Message;
import com.mindrevol.core.modules.chat.entity.MessageDeliveryStatus;
import com.mindrevol.core.modules.chat.entity.MessageReaction;
import com.mindrevol.core.modules.chat.entity.MessageType;
import com.mindrevol.core.modules.chat.repository.ConversationParticipantRepository;
import com.mindrevol.core.modules.chat.repository.MessageReactionRepository;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.box.repository.BoxRepository; // [THÊM MỚI]
import com.mindrevol.core.modules.chat.dto.event.MessageReadEvent;
import com.mindrevol.core.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.core.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.core.modules.chat.dto.response.MessageResponse;
import com.mindrevol.core.modules.chat.entity.*;
import com.mindrevol.core.modules.chat.mapper.ChatMapper;
import com.mindrevol.core.modules.chat.repository.ConversationRepository;
import com.mindrevol.core.modules.chat.repository.MessageRepository;
import com.mindrevol.core.modules.chat.service.ChatService;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.service.NotificationService;
import com.mindrevol.core.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import com.mindrevol.core.modules.user.service.UserBlockService;
import com.mindrevol.core.modules.user.service.UserPresenceService; 
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final BoxRepository boxRepository;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserBlockService userBlockService;
    private final UserPresenceService userPresenceService;
    private final NotificationService notificationService;

    // =====================================================================================
    // HÀM HELPER CHUẨN HÓA (Fix triệt để lỗi mất ghim và lỗi dữ liệu rác từ Mapper)
    // =====================================================================================
    private MessageResponse mapToSafeMessageResponse(Message msg) {
        MessageResponse res = chatMapper.toResponse(msg);
        
        // 1. Ép trạng thái ghim chuẩn xác nhất từ DB
        res.setPinned(msg.isPinned());
        
        // 2. Chuẩn hóa map thả cảm xúc (Reactions)
        if (msg.getReactions() != null && !msg.getReactions().isEmpty()) {
            res.setReactions(msg.getReactions().stream()
                    .collect(Collectors.toMap(r -> r.getUser().getId(), MessageReaction::getReactionType)));
        } else {
            res.setReactions(new HashMap<>());
        }
        
        // 3. Che nội dung nếu tin nhắn bị thu hồi
        if (msg.isDeleted()) {
            res.setContent("Tin nhắn đã bị thu hồi");
        }
        
        return res;
    }
    // =====================================================================================


    @Override
    @Transactional
    public MessageResponse sendMessage(String senderId, SendMessageRequest request) { 
        Conversation conversation;
        User sender = userRepository.getReferenceById(senderId);

        if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện"));
        } else {
            String receiverId = request.getReceiverId(); 
            if (receiverId == null || receiverId.isEmpty()) throw new BadRequestException("Receiver ID không được để trống");
            if (userBlockService.isBlocked(receiverId, senderId)) throw new BadRequestException("Bạn bị chặn.");

            List<Conversation> existingConvs = conversationRepository.findByUsers(senderId, receiverId);
            if (existingConvs.isEmpty()) {
                conversation = createNewConversation(senderId, receiverId);
            } else {
                conversation = existingConvs.get(0);
            }
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .metadata(request.getMetadata())
                .clientSideId(request.getClientSideId())
                .replyToMsgId(request.getReplyToMsgId())
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();

        message = messageRepository.save(message);

        String previewContent = message.getType() == MessageType.IMAGE ? "[Hình ảnh]" : 
                                message.getType() == MessageType.VOICE ? "[Ghi âm]" : 
                                message.getType() == MessageType.FILE ? "[Tệp đính kèm]" : message.getContent();
        conversation.setLastMessageContent(previewContent);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastSenderId(senderId);
        conversationRepository.save(conversation);

        // Bỏ ẩn hội thoại khi có tin nhắn mới
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
        for (ConversationParticipant p : participants) {
            if (p.isHidden()) p.setHidden(false); 
        }
        participantRepository.saveAll(participants);

        // Sử dụng Hàm Helper an toàn
        MessageResponse response = mapToSafeMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat." + conversation.getId(), response);

        // Logic Notification
        if (conversation.getBoxId() == null) {
            User receiver = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(u -> !u.getId().equals(senderId))
                .findFirst().orElse(null);

            if (receiver != null && !userPresenceService.isUserOnline(receiver.getId())) {
                ConversationParticipant receiverParticipant = participantRepository.findByConversationIdAndUserId(conversation.getId(), receiver.getId()).orElse(null);
                if (receiverParticipant == null || !receiverParticipant.isMuted()) {
                    String notiMessage = message.getType() == MessageType.IMAGE ? sender.getFullname() + " đã gửi 1 hình ảnh"
                                       : message.getType() == MessageType.VOICE ? sender.getFullname() + " đã gửi 1 tin nhắn thoại"
                                       : sender.getFullname() + ": " + message.getContent();

                    notificationService.sendAndSaveNotification(
                            receiver.getId(), senderId, NotificationType.DM_NEW_MESSAGE,
                            "Tin nhắn mới", notiMessage, conversation.getId(), sender.getAvatarUrl()
                    );
                }
            }
        }

        return response;
    }

    private Conversation createNewConversation(String senderId, String receiverId) {
        Conversation conv = Conversation.builder()
                .lastMessageAt(LocalDateTime.now())
                .status(ConversationStatus.ACTIVE)
                .build();
        conv = conversationRepository.save(conv);

        participantRepository.save(ConversationParticipant.builder().conversation(conv).user(userRepository.getReferenceById(senderId)).build());
        participantRepository.save(ConversationParticipant.builder().conversation(conv).user(userRepository.getReferenceById(receiverId)).build());

        return conv;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(String userId) { 
        List<Conversation> convs = conversationRepository.findValidConversationsByUserId(userId);
        return convs.stream().map(conv -> mapToConversationResponse(conv, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponse> getConversationMessages(String conversationId, String cursor, int limit) { 
        List<Message> messages;
        PageRequest pageRequest = PageRequest.of(0, limit + 1); 

        if (cursor == null || cursor.isEmpty() || cursor.equals("null")) {
            messages = messageRepository.findByConversationIdOrderByIdDesc(conversationId, pageRequest);
        } else {
            messages = messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, cursor, pageRequest);
        }

        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages.remove(messages.size() - 1);
        }

        String nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

        // Sử dụng Hàm Helper an toàn
        List<MessageResponse> data = messages.stream()
                .map(this::mapToSafeMessageResponse)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(data, nextCursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponse> getMessagesWithUser(String currentUserId, String partnerId, String cursor, int limit) {
        List<Conversation> existingConvs = conversationRepository.findByUsers(currentUserId, partnerId);
        if (existingConvs.isEmpty()) {
            return new CursorPageResponse<>(new ArrayList<>(), null, false);
        }
        return getConversationMessages(existingConvs.get(0).getId(), cursor, limit);
    }

    @Override
    @Transactional
    public void deleteMessage(String messageId, String userId) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Tin nhắn không tồn tại"));
        if (!msg.getSender().getId().equals(userId)) {
            throw new BadRequestException("Bạn chỉ có thể thu hồi tin nhắn của chính mình");
        }
        msg.setDeleted(true);
        messageRepository.save(msg);

        // Sử dụng Hàm Helper an toàn
        MessageResponse response = mapToSafeMessageResponse(msg);
        messagingTemplate.convertAndSend("/topic/chat." + msg.getConversation().getId(), response);
    }

    @Override
    @Transactional
    public MessageResponse reactToMessage(String messageId, String userId, String reactionType) {
        Message msg = messageRepository.findById(messageId).orElseThrow();
        User user = userRepository.getReferenceById(userId);

        MessageReaction reaction = reactionRepository.findByMessageIdAndUserId(messageId, userId)
                .orElse(MessageReaction.builder().message(msg).user(user).build());

        if (reaction.getReactionType() != null && reaction.getReactionType().equals(reactionType)) {
            reactionRepository.delete(reaction);
            msg.getReactions().remove(reaction);
        } else {
            reaction.setReactionType(reactionType);
            reactionRepository.save(reaction);
            if (!msg.getReactions().contains(reaction)) {
                msg.getReactions().add(reaction);
            }
        }

        // Sử dụng Hàm Helper an toàn
        MessageResponse response = mapToSafeMessageResponse(msg);
        messagingTemplate.convertAndSend("/topic/chat." + msg.getConversation().getId(), response);

        return response;
    }

    @Override
    @Transactional
    public MessageResponse editMessage(String messageId, String userId, String newContent) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Tin nhắn không tồn tại"));
        
        if (!msg.getSender().getId().equals(userId)) {
            throw new BadRequestException("Bạn chỉ có thể sửa tin nhắn của chính mình");
        }
        if (msg.isDeleted()) {
            throw new BadRequestException("Không thể sửa tin nhắn đã bị thu hồi");
        }
        if (msg.getType() != MessageType.TEXT) {
            throw new BadRequestException("Chỉ có thể sửa tin nhắn văn bản");
        }

        msg.setContent(newContent);
        Map<String, Object> metadata = msg.getMetadata() != null ? msg.getMetadata() : new HashMap<>();
        metadata.put("isEdited", true);
        msg.setMetadata(metadata);

        messageRepository.save(msg);

        // Sử dụng Hàm Helper an toàn
        MessageResponse response = mapToSafeMessageResponse(msg);
        messagingTemplate.convertAndSend("/topic/chat." + msg.getConversation().getId(), response);
        return response;
    }

    @Override
    @Transactional
    public MessageResponse togglePinMessage(String messageId, String userId) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        // Lật trạng thái ghim
        msg.setPinned(!msg.isPinned());
        messageRepository.save(msg);

        // Sử dụng Hàm Helper an toàn (Đã fix lỗi bị mất ghim)
        MessageResponse response = mapToSafeMessageResponse(msg);
        messagingTemplate.convertAndSend("/topic/chat." + msg.getConversation().getId(), response);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getPinnedMessages(String conversationId) {
        return messageRepository.findPinnedMessages(conversationId).stream()
                .map(this::mapToSafeMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> searchMessages(String conversationId, String keyword) {
        return messageRepository.searchMessages(conversationId, keyword).stream()
                .map(this::mapToSafeMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void togglePinConversation(String conversationId, String userId) {
        ConversationParticipant p = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin hội thoại"));
        p.setPinned(!p.isPinned());
        participantRepository.save(p);
    }

    @Override
    @Transactional
    public void toggleMuteConversation(String conversationId, String userId) {
        ConversationParticipant p = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin hội thoại"));
        p.setMuted(!p.isMuted());
        participantRepository.save(p);
    }

    @Override
    @Transactional
    public void hideConversation(String conversationId, String userId) {
        ConversationParticipant p = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin hội thoại"));
        p.setHidden(true); 
        participantRepository.save(p);
    }

    @Override
    public long getUnreadBadgeCount(String userId) {
        List<Conversation> convs = conversationRepository.findValidConversationsByUserId(userId);
        long total = 0;
        for (Conversation c : convs) {
            ConversationParticipant p = participantRepository.findByConversationIdAndUserId(c.getId(), userId).orElse(null);
            if (p != null && !p.isMuted()) {
                total += messageRepository.countUnreadMessages(c.getId(), userId);
            }
        }
        return total;
    }

    private ConversationResponse mapToConversationResponse(Conversation conv, String currentUserId) {
        ConversationParticipant myParticipant = conv.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(currentUserId))
                .findFirst().orElse(null);

        ConversationResponse.ConversationResponseBuilder builder = ConversationResponse.builder()
            .id(conv.getId())
            .lastMessageContent(conv.getLastMessageContent())
            .lastMessageAt(conv.getLastMessageAt())
            .lastSenderId(conv.getLastSenderId())
            .status(conv.getStatus().name())
            .unreadCount(messageRepository.countUnreadMessages(conv.getId(), currentUserId))
            .isPinned(myParticipant != null && myParticipant.isPinned())
            .isMuted(myParticipant != null && myParticipant.isMuted());

        if (conv.getBoxId() != null) {
            Box box = boxRepository.findById(conv.getBoxId()).orElse(null);
            builder.boxId(conv.getBoxId()).boxName(box != null ? box.getName() : "Không gian").boxAvatar(box != null ? box.getAvatar() : null);
        } else {
            User partner = conv.getParticipants().stream()
                .filter(p -> !p.getUser().getId().equals(currentUserId))
                .map(ConversationParticipant::getUser)
                .findFirst().orElse(null);
            
            if (partner != null) {
                builder.partner(UserSummaryResponse.builder().id(partner.getId()).fullname(partner.getFullname()).avatarUrl(partner.getAvatarUrl()).isOnline(userPresenceService.isUserOnline(partner.getId())).build());
            }
        }
        return builder.build();
    }

    @Override
    @Transactional
    public void markConversationAsRead(String conversationId, String userId) { 
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not in this conversation"));

        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId).orElse(null);
        if (lastMessage != null && (participant.getLastReadMessageId() == null || participant.getLastReadMessageId().compareTo(lastMessage.getId()) < 0)) {
            participant.setLastReadMessageId(lastMessage.getId());
            participantRepository.save(participant);

            messagingTemplate.convertAndSend("/topic/chat." + conversationId + ".read", 
                new MessageReadEvent(conversationId, lastMessage.getId(), userId));
        }
    }
    
    @Override
    public Conversation getConversationById(String id) { 
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện"));
    }
    
    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(String senderId, String receiverId) { 
        if (userBlockService.isBlocked(receiverId, senderId) || userBlockService.isBlocked(senderId, receiverId)) {
            throw new BadRequestException("Không thể bắt đầu cuộc trò chuyện do chặn người dùng.");
        }

        List<Conversation> existingConvs = conversationRepository.findByUsers(senderId, receiverId);
        Conversation conversation;
        
        if (existingConvs.isEmpty()) {
             conversation = createNewConversation(senderId, receiverId);
        } else {
             conversation = existingConvs.get(0);
        }

        return mapToConversationResponse(conversation, senderId);
    }

    @Override
    @Transactional
    public Conversation createBoxConversation(String boxId, String boxName, String creatorId) {
        User creator = userRepository.findById(creatorId).orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        Conversation conv = Conversation.builder()
                .boxId(boxId)
                .lastMessageContent("Không gian chat đã được tạo") 
                .lastMessageAt(LocalDateTime.now())
                .status(ConversationStatus.ACTIVE)
                .build();
        conv = conversationRepository.save(conv);

        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(conv).user(creator).role("ADMIN").build();
        participantRepository.save(participant);

        return conv;
    }

    @Override
    @Transactional
    public void updateBoxConversationInfo(String boxId, String newName) { }

    @Override
    @Transactional
    public void addUserToBoxConversation(String boxId, String userId) {
        Conversation conv = conversationRepository.findByBoxId(boxId).orElse(null);
        if (conv == null) return;
        
        User newUser = userRepository.findById(userId).orElseThrow();
        boolean alreadyInBox = participantRepository.findByConversationIdAndUserId(conv.getId(), userId).isPresent();
        
        if (!alreadyInBox) {
            participantRepository.save(ConversationParticipant.builder().conversation(conv).user(newUser).role("MEMBER").build());

            Message message = Message.builder()
                    .conversation(conv)
                    .sender(newUser) 
                    .content(newUser.getFullname() + " đã tham gia không gian.")
                    .type(MessageType.SYSTEM) 
                    .deliveryStatus(MessageDeliveryStatus.SENT)
                    .build();
            messageRepository.save(message);

            conv.setLastMessageContent(message.getContent());
            conv.setLastMessageAt(LocalDateTime.now());
            conversationRepository.save(conv);
            
            MessageResponse response = mapToSafeMessageResponse(message);
            messagingTemplate.convertAndSend("/topic/chat." + conv.getId(), response);
        }
    }

    @Override
    @Transactional
    public void removeUserFromBoxConversation(String boxId, String userId) {
        Conversation conv = conversationRepository.findByBoxId(boxId).orElse(null);
        if (conv == null) return;

        User leftUser = userRepository.findById(userId).orElseThrow();

        participantRepository.findByConversationIdAndUserId(conv.getId(), userId).ifPresent(participantRepository::delete);

        Message message = Message.builder()
                .conversation(conv)
                .sender(leftUser)
                .content(leftUser.getFullname() + " đã rời khỏi không gian.")
                .type(MessageType.SYSTEM)
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();
        messageRepository.save(message);

        conv.setLastMessageContent(message.getContent());
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        MessageResponse response = mapToSafeMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat." + conv.getId(), response);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getBoxConversation(String boxId, String userId) {
        Conversation conv = conversationRepository.findByBoxId(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện của Không gian này"));
        return mapToConversationResponse(conv, userId);
    }
    
// Thêm vào: src/main/java/com/mindrevol/backend/modules/chat/service/impl/ChatServiceImpl.java
    
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponse> jumpToMessage(String conversationId, String messageId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Message> messages = messageRepository.findMessagesForJump(conversationId, messageId, pageRequest);

        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages.remove(messages.size() - 1);
        }

        String nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

        List<MessageResponse> data = messages.stream()
                .map(this::mapToSafeMessageResponse)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(data, nextCursor, hasNext);
    }
}