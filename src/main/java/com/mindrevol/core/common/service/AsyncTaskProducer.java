package com.mindrevol.core.common.service;

import com.mindrevol.core.modules.notification.dto.EmailTask;
import com.mindrevol.core.modules.notification.dto.PushNotificationTask;
import com.mindrevol.core.modules.notification.dto.WebSocketNotificationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTaskProducer {

    private final RedissonClient redissonClient;
    private static final String EMAIL_QUEUE_NAME = "email_queue";
    private static final String VIDEO_QUEUE_NAME = "video_render_queue"; // Queue mới
    private static final String PUSH_QUEUE_NAME = "notification_push_queue";
    private static final String WEBSOCKET_QUEUE_NAME = "notification_websocket_queue";

    public void submitEmailTask(EmailTask task) {
        RBlockingQueue<EmailTask> queue = redissonClient.getBlockingQueue(EMAIL_QUEUE_NAME);
        queue.add(task);
        log.info("Task submitted to queue: Send email to {}", task.getToEmail());
    }

    public void submitPushNotificationTask(PushNotificationTask task) {
        RBlockingQueue<PushNotificationTask> queue = redissonClient.getBlockingQueue(PUSH_QUEUE_NAME);
        queue.add(task);
    }

    public void submitWebSocketNotificationTask(WebSocketNotificationTask task) {
        RBlockingQueue<WebSocketNotificationTask> queue = redissonClient.getBlockingQueue(WEBSOCKET_QUEUE_NAME);
        queue.add(task);
    }

    // --- THÊM HÀM NÀY ---
//    public void submitVideoTask(VideoTask task) {
//        RBlockingQueue<VideoTask> queue = redissonClient.getBlockingQueue(VIDEO_QUEUE_NAME);
//        queue.add(task);
//        log.info("Task submitted to VIDEO queue for User {} Journey {}", task.getUserId(), task.getJourneyId());
//    }
}