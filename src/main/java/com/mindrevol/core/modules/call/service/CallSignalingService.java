package com.mindrevol.core.modules.call.service;

import com.mindrevol.core.common.exception.BadRequestException;
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

        // Bắn tín hiệu WebSocket cho App đang mở
        sendWebSocket(receiverId, "INCOMING_CALL", session);
        
        // Bắn FCM Push Notification cho App đang tắt
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
            
            // 🔥 TĂNG THỜI GIAN SỐNG TRÊN REDIS KHI CHẤP NHẬN (Tránh bị expire giữa chừng)
            redisTemplate.opsForValue().set(key, session, 2, TimeUnit.HOURS);
            
            // Đánh dấu người dùng đang bận
            redisTemplate.opsForValue().set(USER_IN_CALL_PREFIX + session.getCallerId(), "1", 2, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(USER_IN_CALL_PREFIX + receiverId, "1", 2, TimeUnit.HOURS);
            
            sendWebSocket(session.getCallerId(), "CALL_ACCEPTED", session);
        } 
        else if ("REJECT".equals(action)) {
            saveCallLog(session, "REJECTED", 0);
            // Thay vì xóa ngay, hãy để nó tự hết hạn sau vài giây để tránh lỗi race condition
            redisTemplate.expire(key, 5, TimeUnit.SECONDS); 
            sendWebSocket(session.getCallerId(), "CALL_REJECTED", session);
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

 // 🔥 HÀM MỚI: BẮT ĐẦU HOẶC THAM GIA CUỘC GỌI NHÓM TRONG BOX
    public CallSession initiateBoxCall(String callerId, String boxId, String conversationId, String type) {
        String key = BOX_CALL_PREFIX + boxId;
        
        // 1. Kiểm tra xem Box này đang có cuộc gọi nào diễn ra không
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            // Nếu có, trả về phòng hiện tại để người dùng Join vào
            return (CallSession) redisTemplate.opsForValue().get(key);
        }

        // 2. Nếu chưa có, tạo phòng mới
        User caller = userRepository.findById(callerId).orElseThrow();
        String roomId = "room_box_" + boxId + "_" + UUID.randomUUID().toString().substring(0, 5);

        CallSession session = CallSession.builder()
                .roomId(roomId)
                .conversationId(conversationId)
                .callerId(callerId)
                .receiverId(boxId) // Mượn trường này lưu boxId
                .callerName(caller.getFullname())
                .callerAvatar(caller.getAvatarUrl())
                .callType(type)
                .status("IN_PROGRESS") // Nhóm thì vào thẳng luôn, không có RINGING
                .timestamp(System.currentTimeMillis())
                .startTime(System.currentTimeMillis())
                .isGroup(true)
                .build();

        // Lưu phòng lên Redis (Sống tối đa 4 tiếng nếu không ai dọn dẹp)
        redisTemplate.opsForValue().set(key, session, 4, TimeUnit.HOURS);

        // Lưu tin nhắn "Cuộc gọi nhóm đã bắt đầu" vào Chat
        saveCallLog(session, "GROUP_STARTED", 0);

        // Bắn WebSocket báo cho mọi người trong Box biết để hiện nút "Tham gia"
        NotificationResponse payload = NotificationResponse.builder()
                .type("BOX_CALL_STARTED")
                .referenceId(roomId)
                .messageArgs(boxId + "|" + type + "|" + caller.getFullname())
                .build();
        
        // Gửi chung vào topic của conversation đó
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, payload);

        return session;
    }

    // 🔥 HÀM MỚI: KẾT THÚC HOẶC RỜI CUỘC GỌI NHÓM
    public void leaveBoxCall(String boxId, String roomId) {
        // Trong kiến trúc SFU của Zego, việc đếm số người trong phòng nên được làm ở Frontend.
        // Tuy nhiên để đơn giản, nếu người khởi tạo cúp máy hoặc phòng trống, ta xóa key Redis.
        // Tạm thời cung cấp API dọn dẹp key này khi cuộc gọi nhóm thực sự kết thúc.
        String key = BOX_CALL_PREFIX + boxId;
        CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
        
        if (session != null && session.getRoomId().equals(roomId)) {
            redisTemplate.delete(key);
            
            // Bắn tín hiệu phòng đã đóng
            NotificationResponse payload = NotificationResponse.builder()
                    .type("BOX_CALL_ENDED")
                    .referenceId(roomId)
                    .messageArgs(boxId)
                    .build();
            messagingTemplate.convertAndSend("/topic/conversations/" + session.getConversationId(), payload);
        }
    }

    // Sửa lại hàm saveCallLog một chút để hỗ trợ Group
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
                .messageArgs(session.getCallerId() + "|" + session.getCallerName() + "|" + session.getCallType() + "|" + session.getCallerAvatar())
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
    
    /**
     * Dùng cho CallDisconnectListener khi WebSocket bị ngắt đột ngột
     */
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
            // Đảm bảo dọn dẹp cờ BUSY nếu kẹt
            redisTemplate.delete(USER_IN_CALL_PREFIX + userId);
        }
    }

    /**
     * Dùng cho CallCleanupJob quét ngầm định kỳ (BẢN TEST - XÓA LIỀN SAU 5 GIÂY)
     */
    public void cleanupStaleCalls() {
        long now = System.currentTimeMillis();
        
        // 1. DỌN DẸP CÁC PHÒNG GỌI BỊ KẸT
        Set<String> keys = redisTemplate.keys(CALL_KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                CallSession session = (CallSession) redisTemplate.opsForValue().get(key);
                if (session != null) {
                    // Đã hạ xuống 5000 ms (5 giây) để test
                    boolean isStaleRinging = "RINGING".equals(session.getStatus()) && (now - session.getTimestamp() > 5000);
                    boolean isStaleInProgress = "IN_PROGRESS".equals(session.getStatus()) && (now - session.getStartTime() > 5000);
                    
                    // Nếu bạn muốn XÓA SẠCH SÀNH SANH KHÔNG CẦN CHỜ GIÂY NÀO, bỏ luôn điều kiện thời gian:
                    // boolean isStaleRinging = "RINGING".equals(session.getStatus());
                    // boolean isStaleInProgress = "IN_PROGRESS".equals(session.getStatus());

                    if (isStaleRinging || isStaleInProgress) {
                        log.info("Dọn dẹp Stale Call (TEST MODE): {}", session.getRoomId());
                        endCall(session.getRoomId());
                    }
                }
            }
        }

        // 2. DỌN DẸP CỜ BÁO BẬN MỒ CÔI (GHOST BUSY FLAGS)
        // Phần này vốn dĩ đã là "xóa liền" ngay khi nó phát hiện không có session ở trên
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

                // Nếu không có session -> Xóa cờ ngay lập tức
                if (!isActuallyInCall) {
                    log.info("Dọn dẹp cờ báo bận mồ côi cho user: {}", userId);
                    redisTemplate.delete(busyKey);
                }
            }
        }
    }
}