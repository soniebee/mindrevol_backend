package com.mindrevol.core.modules.mood.job;

import com.mindrevol.core.modules.mood.repository.MoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoodCleanupJob {

    private final MoodRepository moodRepository;

    // 🔥 PRO MAX: Chạy tự động vào phút thứ 0 của mỗi giờ (VD: 1:00, 2:00, 3:00...)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredMoods() {
        log.info("🧹 Bắt đầu dọn dẹp rác (Mood đã quá 24h)...");
        try {
            LocalDateTime now = LocalDateTime.now();

            // Xóa rác Reaction trước
            moodRepository.hardDeleteExpiredReactions(now);
            // Xóa rác Mood sau
            moodRepository.hardDeleteExpiredMoods(now);

            log.info("✨ Đã dọn dẹp sạch sẽ CSDL.");
        } catch (Exception e) {
            log.error("❌ Lỗi khi dọn rác Mood", e);
        }
    }
}