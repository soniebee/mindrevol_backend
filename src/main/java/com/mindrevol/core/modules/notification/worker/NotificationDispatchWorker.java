package com.mindrevol.core.modules.notification.worker;

import com.mindrevol.core.modules.notification.dto.PushNotificationTask;
import com.mindrevol.core.modules.notification.dto.WebSocketNotificationTask;
import com.mindrevol.core.modules.notification.service.FirebaseService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.entity.UserSettings;
import com.mindrevol.core.modules.user.repository.UserRepository;
import com.mindrevol.core.modules.user.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchWorker {

    private static final String PUSH_QUEUE_NAME = "notification_push_queue";
    private static final String WEBSOCKET_QUEUE_NAME = "notification_websocket_queue";
    private static final long REDIS_ERROR_LOG_COOLDOWN_MS = 300000L; // 5 phút
    private static final int MAX_RETRY = 3;

    private final RedissonClient redissonClient;
    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserService userService;

    private volatile long lastPushRedisErrorLogAt = 0L;
    private volatile long lastWebSocketRedisErrorLogAt = 0L;
    private volatile String lastPushErrorMsg = null;
    private volatile String lastWebSocketErrorMsg = null;

    private final ExecutorService pushExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService webSocketExecutor = Executors.newFixedThreadPool(3);

    @PostConstruct
    public void startWorker() {
        log.info("NotificationDispatchWorker started");
        for (int i = 0; i < 3; i++) {
            pushExecutor.submit(this::processPushQueue);
            webSocketExecutor.submit(this::processWebSocketQueue);
        }
    }

    @PreDestroy
    public void stopWorker() {
        log.info("NotificationDispatchWorker stopping");
        pushExecutor.shutdownNow();
        webSocketExecutor.shutdownNow();
        awaitTermination(pushExecutor, "Push worker");
        awaitTermination(webSocketExecutor, "WebSocket worker");
        log.info("NotificationDispatchWorker stopped");
    }

    private void awaitTermination(ExecutorService executor, String name) {
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processPushQueue() {
        if (redissonClient.isShutdown() || redissonClient.isShuttingDown()) {
            return;
        }
        RBlockingQueue<PushNotificationTask> queue = redissonClient.getBlockingQueue(PUSH_QUEUE_NAME);
        while (!Thread.currentThread().isInterrupted()) {
            PushNotificationTask task = null;
            try {
                if (redissonClient.isShutdown()) {
                    break;
                }
                task = queue.poll(2, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                // DND check before sending notification
                UserSettings settings = userService.getNotificationSettings(task.getRecipientId());
                if (settings != null && Boolean.TRUE.equals(settings.getDndEnabled()) && settings.getDndStartHour() != null && settings.getDndEndHour() != null) {
                    int start = settings.getDndStartHour();
                    int end = settings.getDndEndHour();
                    int currentHour = ZonedDateTime.now(ZoneId.systemDefault()).getHour();
                    boolean dndActive = (start <= end) ? (currentHour >= start && currentHour < end) : (currentHour >= start || currentHour < end);
                    if (dndActive) {
                        // Skip sending and do not retry if DND is active
                        continue;
                    }
                }
                boolean delivered = firebaseService.sendNotification(
                        task.getFcmToken(),
                        task.getTitle(),
                        task.getMessage(),
                        task.getDataPayload()
                );
                if (!delivered) {
                    continue;
                }
            } catch (RedissonShutdownException e) {
                // Không log gì khi worker shutdown
                break;
            } catch (Exception e) {
                if (task != null) {
                    retryPushTask(task, queue);
                }
                long now = System.currentTimeMillis();
                String msg = e.getMessage();
                if ((lastPushErrorMsg == null || !lastPushErrorMsg.equals(msg)) || (now - lastPushRedisErrorLogAt >= REDIS_ERROR_LOG_COOLDOWN_MS)) {
                    lastPushErrorMsg = msg;
                    lastPushRedisErrorLogAt = now;
                    log.error("Notification push worker error: {}", msg, e);
                } else {
                    // Chỉ log ngắn gọn nếu lỗi lặp lại trong cooldown
                    log.warn("Notification push worker error (repeat): {}", msg);
                }
            }
        }
    }

    private void processWebSocketQueue() {
        if (redissonClient.isShutdown() || redissonClient.isShuttingDown()) {
            return;
        }
        RBlockingQueue<WebSocketNotificationTask> queue = redissonClient.getBlockingQueue(WEBSOCKET_QUEUE_NAME);
        while (!Thread.currentThread().isInterrupted()) {
            WebSocketNotificationTask task = null;
            try {
                if (redissonClient.isShutdown()) {
                    break;
                }
                task = queue.poll(2, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                String principalName = userRepository.findById(task.getRecipientId())
                        .map(User::getEmail)
                        .orElse(task.getRecipientId());
                messagingTemplate.convertAndSendToUser(principalName, "/queue/notifications", task.getResponse());
                if (!principalName.equals(task.getRecipientId())) {
                    messagingTemplate.convertAndSendToUser(task.getRecipientId(), "/queue/notifications", task.getResponse());
                }
            } catch (RedissonShutdownException e) {
                // Không log gì khi worker shutdown
                break;
            } catch (Exception e) {
                if (task != null) {
                    retryWebSocketTask(task, queue);
                }
                long now = System.currentTimeMillis();
                String msg = e.getMessage();
                if ((lastWebSocketErrorMsg == null || !lastWebSocketErrorMsg.equals(msg)) || (now - lastWebSocketRedisErrorLogAt >= REDIS_ERROR_LOG_COOLDOWN_MS)) {
                    lastWebSocketErrorMsg = msg;
                    lastWebSocketRedisErrorLogAt = now;
                    log.error("Notification websocket worker error: {}", msg, e);
                } else {
                    // Chỉ log ngắn gọn nếu lỗi lặp lại trong cooldown
                    log.warn("Notification websocket worker error (repeat): {}", msg);
                }
            }
        }
    }

    private void retryPushTask(PushNotificationTask task, RBlockingQueue<PushNotificationTask> queue) {
        if (task.getRetryCount() >= MAX_RETRY || redissonClient.isShutdown()) {
            return;
        }
        task.setRetryCount(task.getRetryCount() + 1);
        queue.add(task);
    }

    private void retryWebSocketTask(WebSocketNotificationTask task, RBlockingQueue<WebSocketNotificationTask> queue) {
        if (task.getRetryCount() >= MAX_RETRY || redissonClient.isShutdown()) {
            return;
        }
        task.setRetryCount(task.getRetryCount() + 1);
        queue.add(task);
    }
}
