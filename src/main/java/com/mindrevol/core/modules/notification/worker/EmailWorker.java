package com.mindrevol.core.modules.notification.worker;

import com.mindrevol.core.modules.notification.dto.EmailTask;
import com.mindrevol.core.modules.notification.service.EmailService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {

    private final RedissonClient redissonClient;
    private final EmailService emailService;

    private static final String EMAIL_QUEUE_NAME = "email_queue";
    private static final long REDIS_ERROR_LOG_COOLDOWN_MS = 30000L;

    private volatile long lastRedisErrorLogAt = 0L;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PostConstruct
    public void startWorker() {
        for (int i = 0; i < 5; i++) {
            executorService.submit(this::processQueue);
        }
    }

    @PreDestroy
    public void stopWorker() {
        log.info("Stopping Email Worker...");
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Email Worker did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processQueue() {
        if (redissonClient.isShutdown() || redissonClient.isShuttingDown()) {
            return;
        }

        RBlockingQueue<EmailTask> queue = redissonClient.getBlockingQueue(EMAIL_QUEUE_NAME);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (redissonClient.isShutdown()) break;

                EmailTask task = queue.poll();

                if (task != null) {
                    handleTask(task, queue);
                } else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (RedissonShutdownException e) {
                break;
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                if (now - lastRedisErrorLogAt >= REDIS_ERROR_LOG_COOLDOWN_MS) {
                    lastRedisErrorLogAt = now;
                    log.warn("EmailWorker Redis connection unstable: {}", e.getMessage());
                }
                try { Thread.sleep(5000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private void handleTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        try {
            emailService.sendEmail(task.getToEmail(), task.getSubject(), task.getContent());
            log.debug("Email sent successfully to {}", task.getToEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}", task.getToEmail(), e);
            retryTask(task, queue);
        }
    }

    private void retryTask(EmailTask task, RBlockingQueue<EmailTask> queue) {
        if (task.getRetryCount() < 3) {
            task.setRetryCount(task.getRetryCount() + 1);
            log.debug("Retrying email task for {} (attempt {})", task.getToEmail(), task.getRetryCount());
            if (!redissonClient.isShutdown()) {
                queue.add(task);
            }
        } else {
            log.error("💀 Email task for {} failed after 3 attempts. Discarding.", task.getToEmail());
        }
    }
}
