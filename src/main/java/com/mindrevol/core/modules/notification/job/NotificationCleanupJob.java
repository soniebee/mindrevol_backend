package com.mindrevol.core.modules.notification.job;

import com.mindrevol.core.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    // Chạy tự động vào lúc 2h sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Bắt đầu dọn dẹp thông báo cũ (Notification Cleanup Job)...");

        // 1. Xóa cứng thông báo ĐÃ ĐỌC sau 30 ngày
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deletedRead = notificationRepository.deleteOldReadNotifications(thirtyDaysAgo);

        // 2. Xóa cứng thông báo CHƯA ĐỌC sau 60 ngày
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        int deletedUnread = notificationRepository.deleteOldUnreadNotifications(sixtyDaysAgo);

        log.info("Hoàn tất dọn dẹp hệ thống: Đã xóa {} thông báo đã đọc, {} thông báo chưa đọc.", deletedRead, deletedUnread);
    }
}