package com.mindrevol.core.modules.call.service;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.modules.box.entity.BoxMember;
import com.mindrevol.core.modules.box.repository.BoxMemberRepository;
import com.mindrevol.core.modules.call.dto.CallSession;
import com.mindrevol.core.modules.chat.entity.Conversation;
import com.mindrevol.core.modules.chat.entity.Message;
import com.mindrevol.core.modules.chat.entity.MessageDeliveryStatus;
import com.mindrevol.core.modules.chat.entity.MessageType;
import com.mindrevol.core.modules.chat.repository.ConversationRepository;
import com.mindrevol.core.modules.chat.repository.MessageRepository;
import com.mindrevol.core.modules.notification.dto.PushNotificationTask;
import com.mindrevol.core.modules.notification.dto.response.NotificationResponse;
import com.mindrevol.core.modules.notification.service.NotificationDispatchService;
import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallSignalingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final BoxMemberRepository boxMemberRepository;
    private final AsyncTaskProducer asyncTaskProducer;

    private static final String CALL_KEY_PREFIX = "call_session:";
    private static final String USER_IN_CALL_PREFIX = "user_in_call:"; 
    private static final String BOX_CALL_PREFIX = "box_call_room:";

    public CallSession initiateCall(String callerId, String receiverId, String type, String conversationId) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(USER_IN_CALL_PREFIX + receiverId))) {
            throw new BadRequestException("Người dùng đang bận trong cuộc gọi khác.");
        }

        User caller = userRepository.findById(callerId).orElseThrow();
        String roomId = "room_" + UUID.randomUUID().toString();

        CallSession session = CallSession.builder()
                .roomId(roomId).conversationId(conversationId)
                .callerId(callerId).receiverId(receiverId)
                .callerName(caller.getFullname()).callerAvatar(caller.getAvatarUrl())
                .callType(type).status("RINGING").timestamp(System.currentTimeMillis())
                .build();

        redisTemplate.opsForValue().set(CALL_KEY_PREFIX + roomId, session, 45, TimeUnit.SECONDS);

        sendWebSocket(receiverId, "INCOMING_CALL", session);
        sendOfflinePushNotification(receiverId, session);

        return session;
    }

    public void respondToCall(String roomId, String receiverId, String action) {
        String key = CALL_KEY_PREFIX + roomId;
        CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
        
        if (session == null) throw new BadRequestException("Cuộc gọi đã kết thúc.");

        if ("ACCEPT".equals(action)) {
            session.setStatus("IN_PROGRESS");
            session.setStartTime(System.currentTimeMillis());
            
            redisTemplate.opsForValue().set(key, session, 2, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(USER_IN_CALL_PREFIX + session.getCallerId(), "1", 2, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(USER_IN_CALL_PREFIX + receiverId, "1", 2, TimeUnit.HOURS);
            
            sendWebSocket(session.getCallerId(), "CALL_ACCEPTED", session);
        } 
        else if ("REJECT".equals(action)) {
            saveCallLog(session, "REJECTED", 0);
            redisTemplate.expire(key, 5, TimeUnit.SECONDS); 
            sendWebSocket(session.getCallerId(), "CALL_REJECTED", session);
        }
    }

    // 🔥 HÀM MỚI: Xử lý khi có ai đó bấm Nghe máy cuộc gọi nhóm
    public void respondToBoxCall(String roomId, String boxId, String receiverId, String action) {
        String key = BOX_CALL_PREFIX + boxId;
        CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
        
        if (session == null) throw new BadRequestException("Cuộc gọi nhóm đã kết thúc.");

        if ("ACCEPT".equals(action)) {
            // Khi có 1 người bấm Accept, bắn WebSocket về cho NGƯỜI GỌI (Caller) 
            sendWebSocket(session.getCallerId(), "BOX_CALL_ACCEPTED", session);
        }
    }

    public void endCall(String roomId) {
        String key = CALL_KEY_PREFIX + roomId;
        CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
        if (session != null) {
            long durationMs = session.getStartTime() > 0 ? System.currentTimeMillis() - session.getStartTime() : 0;
            String status = durationMs > 0 ? "COMPLETED" : "MISSED"; 
            
            saveCallLog(session, status, durationMs / 1000);

            redisTemplate.delete(USER_IN_CALL_PREFIX + session.getCallerId());
            redisTemplate.delete(USER_IN_CALL_PREFIX + session.getReceiverId());
            redisTemplate.delete(key);
            
            sendWebSocket(session.getCallerId(), "CALL_ENDED", session);
            sendWebSocket(session.getReceiverId(), "CALL_ENDED", session);
        }
    }

    public CallSession initiateBoxCall(String callerId, String boxId, String conversationId, String type) {
        String key = BOX_CALL_PREFIX + boxId;
        
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return (CallSession) redisTemplate.opsForValue().get(key);
        }

        User caller = userRepository.findById(callerId).orElseThrow();
        String roomId = "room_box_" + boxId + "_" + UUID.randomUUID().toString().substring(0, 5);

        CallSession session = CallSession.builder()
                .roomId(roomId)
                .conversationId(conversationId)
                .callerId(callerId)
                .receiverId(boxId) 
                .callerName(caller.getFullname())
                .callerAvatar(caller.getAvatarUrl())
                .callType(type)
                .status("IN_PROGRESS") 
                .timestamp(System.currentTimeMillis())
                .startTime(System.currentTimeMillis())
                .isGroup(true)
                .build();

        redisTemplate.opsForValue().set(key, session, 4, TimeUnit.HOURS);
        saveCallLog(session, "GROUP_STARTED", 0);

        List<BoxMember> members = boxMemberRepository.findByBoxId(boxId);
        for (BoxMember member : members) {
            String targetUserId = member.getUser().getId();
            
            if (!targetUserId.equals(callerId)) {
                sendWebSocket(targetUserId, "BOX_CALL_STARTED", session);
                sendOfflinePushNotification(targetUserId, session);
            }
        }

        return session;
    }

    public void leaveBoxCall(String boxId, String roomId) {
        String key = BOX_CALL_PREFIX + boxId;
        CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
        
        if (session != null && session.getRoomId().equals(roomId)) {
            redisTemplate.delete(key);
            
            NotificationResponse payload = NotificationResponse.builder()
                    .type("BOX_CALL_ENDED")
                    .referenceId(roomId)
                    .messageArgs(boxId)
                    .build();
            messagingTemplate.convertAndSend("/topic/conversations/" + session.getConversationId(), payload);
        }
    }

    private void saveCallLog(CallSession session, String status, long durationSeconds) {
        try {
            String content = String.format("%s|%s|%d", session.getCallType(), status, durationSeconds);
            Conversation conv = conversationRepository.findById(session.getConversationId()).orElseThrow();
            User sender = userRepository.findById(session.getCallerId()).orElseThrow();

            Message callLogMessage = Message.builder()
                    .conversation(conv).sender(sender)
                    .type(MessageType.CALL_LOG).content(content)
                    .deliveryStatus(MessageDeliveryStatus.SENT)
                    .clientSideId(String.valueOf(System.currentTimeMillis()))
                    .build();
            
            messageRepository.save(callLogMessage);
            
            String logText = status.equals("GROUP_STARTED") ? "[Bắt đầu cuộc gọi nhóm]" : typeToVietnamese(session.getCallType()) + " " + statusToVietnamese(status);
            conv.setLastMessageContent(logText);
            conv.setLastMessageAt(LocalDateTime.now());
            conv.setLastSenderId(sender.getId());
            conversationRepository.save(conv);

        } catch (Exception e) {
            log.error("Lỗi khi lưu lịch sử cuộc gọi", e);
        }
    }

    private String typeToVietnamese(String type) {
        return "VIDEO".equalsIgnoreCase(type) ? "[Cuộc gọi Video]" : "[Cuộc gọi Thoại]";
    }

    private String statusToVietnamese(String status) {
        if ("MISSED".equals(status)) return "nhỡ";
        if ("REJECTED".equals(status)) return "bị từ chối";
        return "";
    }

    private void sendWebSocket(String userId, String type, CallSession session) {
        NotificationResponse payload = NotificationResponse.builder()
                .type(type)
                .referenceId(session.getRoomId())
                // 🔥 ĐÃ SỬA: Gửi thêm receiverId (boxId) vào cuối args
                .messageArgs(session.getCallerId() + "|" + session.getCallerName() + "|" + session.getCallType() + "|" + session.getCallerAvatar() + "|" + session.getReceiverId())
                .build();

        String principalName = userRepository.findById(userId).map(User::getEmail).orElse(userId);
        messagingTemplate.convertAndSendToUser(principalName, "/queue/notifications", payload);
    }

    private void sendOfflinePushNotification(String receiverId, CallSession session) {
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver != null && receiver.getFcmToken() != null && !receiver.getFcmToken().isBlank()) {
            Map<String, String> dataPayload = new HashMap<>();
            dataPayload.put("type", "CALL_INVITE");
            dataPayload.put("roomId", session.getRoomId());
            dataPayload.put("callType", session.getCallType());
            dataPayload.put("callerName", session.getCallerName());
            dataPayload.put("callerAvatar", session.getCallerAvatar());

            asyncTaskProducer.submitPushNotificationTask(PushNotificationTask.builder()
                    .recipientId(receiverId)
                    .fcmToken(receiver.getFcmToken())
                    .title("📞 Cuộc gọi " + (session.getCallType().equals("VIDEO") ? "Video" : "Thoại"))
                    .message(session.getCallerName() + " đang gọi cho bạn...")
                    .dataPayload(dataPayload)
                    .retryCount(0)
                    .build());
        }
    }
    
    public void forceEndActiveCallsByUserId(String userId) {
        Set<String> keys = redisTemplate.keys(CALL_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
                if (session != null && (userId.equals(session.getCallerId()) || userId.equals(session.getReceiverId()))) {
                    log.info("Phát hiện mạng ngắt, tự động End Call cho Room: {}", session.getRoomId());
                    endCall(session.getRoomId());
                }
            }
        } else {
            redisTemplate.delete(USER_IN_CALL_PREFIX + userId);
        }
    }

    public void cleanupStaleCalls() {
        long now = System.currentTimeMillis();
        
        Set<String> keys = redisTemplate.keys(CALL_KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
                if (session != null) {
                    boolean isStaleRinging = "RINGING".equals(session.getStatus()) && (now - session.getTimestamp() > 5000);
                    boolean isStaleInProgress = "IN_PROGRESS".equals(session.getStatus()) && (now - session.getStartTime() > 5000);

                    if (isStaleRinging || isStaleInProgress) {
                        log.info("Dọn dẹp Stale Call (TEST MODE): {}", session.getRoomId());
                        endCall(session.getRoomId());
                    }
                }
            }
        }

        Set<String> busyKeys = redisTemplate.keys(USER_IN_CALL_PREFIX + "*");
        if (busyKeys != null) {
            for (String busyKey : busyKeys) {
                String userId = busyKey.replace(USER_IN_CALL_PREFIX, "");
                boolean isActuallyInCall = false;
                
                if (keys != null) {
                    for (String key : keys) {
                        CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
                        if (session != null && (userId.equals(session.getCallerId()) || userId.equals(session.getReceiverId()))) {
                            isActuallyInCall = true;
                            break;
                        }
                    }
                }

                if (!isActuallyInCall) {
                    log.info("Dọn dẹp cờ báo bận mồ côi cho user: {}", userId);
                    redisTemplate.delete(busyKey);
                }
            }
        }
    }
}