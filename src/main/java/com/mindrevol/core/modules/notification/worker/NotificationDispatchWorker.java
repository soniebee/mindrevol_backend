package com.mindrevol.core.modules.notification.worker;

import com.mindrevol.core.modules.notification.dto.PushNotificationTask;
import com.mindrevol.core.modules.notification.dto.WebSocketNotificationTask;
import com.mindrevol.core.modules.notification.service.FirebaseService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchWorker {

    private static final String PUSH_QUEUE_NAME = "notification_push_queue";
    private static final String WEBSOCKET_QUEUE_NAME = "notification_websocket_queue";
    private static final long REDIS_ERROR_LOG_COOLDOWN_MS = 30000L;
    private static final int MAX_RETRY = 3;

    private final RedissonClient redissonClient;
    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private volatile long lastPushRedisErrorLogAt = 0L;
    private volatile long lastWebSocketRedisErrorLogAt = 0L;

    private final ExecutorService pushExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService webSocketExecutor = Executors.newFixedThreadPool(3);

    @PostConstruct
    public void startWorker() {
        for (int i = 0; i < 3; i++) {
            pushExecutor.submit(this::processPushQueue);
            webSocketExecutor.submit(this::processWebSocketQueue);
        }
    }

    @PreDestroy
    public void stopWorker() {
        pushExecutor.shutdownNow();
        webSocketExecutor.shutdownNow();
        awaitTermination(pushExecutor, "Push worker");
        awaitTermination(webSocketExecutor, "WebSocket worker");
    }

    private void awaitTermination(ExecutorService executor, String name) {
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("{} did not terminate gracefully", name);
            }
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
                break;
            } catch (Exception e) {
                if (task != null) {
                    retryPushTask(task, queue);
                }
                long now = System.currentTimeMillis();
                if (now - lastPushRedisErrorLogAt >= REDIS_ERROR_LOG_COOLDOWN_MS) {
                    lastPushRedisErrorLogAt = now;
                    log.warn("Notification push worker issue: {}", e.getMessage());
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
                    // Fallback for deployments that use userId as websocket principal.
                    messagingTemplate.convertAndSendToUser(task.getRecipientId(), "/queue/notifications", task.getResponse());
                }
            } catch (RedissonShutdownException e) {
                break;
            } catch (Exception e) {
                if (task != null) {
                    retryWebSocketTask(task, queue);
                }
                long now = System.currentTimeMillis();
                if (now - lastWebSocketRedisErrorLogAt >= REDIS_ERROR_LOG_COOLDOWN_MS) {
                    lastWebSocketRedisErrorLogAt = now;
                    log.warn("Notification websocket worker issue: {}", e.getMessage());
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




