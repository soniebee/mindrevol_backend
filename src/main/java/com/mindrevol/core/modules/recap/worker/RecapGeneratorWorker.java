package com.mindrevol.core.modules.recap.worker;

import com.mindrevol.core.common.constant.AppConstants;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import com.mindrevol.core.modules.notification.service.NotificationService;
import com.mindrevol.core.modules.recap.dto.RecapTask;
import com.mindrevol.core.modules.recap.entity.Recap;
import com.mindrevol.core.modules.recap.entity.RecapStatus;
import com.mindrevol.core.modules.recap.repository.RecapRepository;
import com.mindrevol.core.modules.recap.service.strategy.RecapGeneratorStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RecapGeneratorWorker {

    private final RedissonClient redissonClient;
    private final RecapRepository recapRepository;
    private final CheckinRepository checkinRepository;
    private final NotificationService notificationService;
    private final RecapGeneratorStrategy recapGenerator;

    private final ExecutorService workerExecutor = Executors.newFixedThreadPool(2);

    // Constructor tùy chỉnh để xử lý lỗi 2 bean và tự động tiêm "cloudinaryRecap"
    public RecapGeneratorWorker(
            RedissonClient redissonClient,
            RecapRepository recapRepository,
            CheckinRepository checkinRepository,
            NotificationService notificationService,
            @Qualifier("ffmpegRecap") RecapGeneratorStrategy recapGenerator) {
        this.redissonClient = redissonClient;
        this.recapRepository = recapRepository;
        this.checkinRepository = checkinRepository;
        this.notificationService = notificationService;
        this.recapGenerator = recapGenerator;
    }

    @PostConstruct
    public void startWorker() {
        log.info("RecapGeneratorWorker started");
        for (int i = 0; i < 2; i++) {
            workerExecutor.submit(this::processQueue);
        }
    }

    @PreDestroy
    public void stopWorker() {
        workerExecutor.shutdownNow();
    }

    private void processQueue() {
        if (redissonClient.isShutdown()) return;
        
        RBlockingQueue<RecapTask> queue = redissonClient.getBlockingQueue(AppConstants.QUEUE_RECAP_GENERATION);
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (redissonClient.isShutdown()) break;
                
                RecapTask task = queue.poll(2, TimeUnit.SECONDS);
                if (task == null) continue;

                executeRecapGeneration(task);

            } catch (RedissonShutdownException e) {
                break;
            } catch (Exception e) {
                log.error("Lỗi worker xử lý Recap Queue", e);
            }
        }
    }

    private void executeRecapGeneration(RecapTask task) {
        Recap recap = recapRepository.findById(task.getRecapId()).orElse(null);
        if (recap == null || recap.getStatus() != RecapStatus.PENDING) return;

        try {
            recap.setStatus(RecapStatus.PROCESSING);
            recapRepository.save(recap);

            // 1. Tải ảnh từ Database dựa trên Config của người dùng truyền từ Modal
            List<Checkin> checkins;
            
            if (task.getSelectedCheckinIds() != null && !task.getSelectedCheckinIds().isEmpty()) {
                // Người dùng đã tick chọn từng ảnh trên UI
                checkins = checkinRepository.findAllById(task.getSelectedCheckinIds());
            } else if ("ME".equals(task.getFilterType())) {
                // Chỉ lấy ảnh của người dùng hiện tại
                checkins = checkinRepository.findMediaByJourneyIdAndUserId(task.getJourneyId(), task.getUserId());
            } else {
                // Toàn bộ Hành trình (Của tất cả mọi người)
                checkins = checkinRepository.findMediaByJourneyIdForRecap(task.getJourneyId());
            }

            List<String> imageUrls = checkins.stream()
                    .map(Checkin::getImageUrl)
                    .filter(url -> url != null && !url.isEmpty())
                    .limit(60) // Giới hạn tối đa để xử lý video không bị lỗi out-of-memory hoặc quota
                    .collect(Collectors.toList());

            if (imageUrls.isEmpty()) {
                throw new IllegalStateException("Không tìm thấy bức ảnh nào phù hợp để tạo video.");
            }

            // 2. Chạy Strategy render Video. (Truyền tốc độ ms vào, mặc định 500ms nếu bị null)
            int delayMs = (task.getSpeedDelayMs() != null) ? task.getSpeedDelayMs() : 500;
            String finalVideoUrl = recapGenerator.generateVideo(imageUrls, null, delayMs);

            // 3. Hoàn tất lưu DB
            recap.setVideoUrl(finalVideoUrl);
            recap.setThumbnailUrl(imageUrls.get(0)); // Lấy ảnh đầu làm bìa
            recap.setStatus(RecapStatus.COMPLETED);
            recapRepository.save(recap);

            // 4. Bắn Notification thông báo cho người dùng
            notificationService.sendAndSaveNotification(
                    task.getUserId(),
                    null, 
                    NotificationType.RECAP_READY,
                    "🎬 Thước phim kỷ niệm đã ra lò!",
                    "Vào xem ngay video recap bạn vừa tạo nhé.",
                    recap.getId(),
                    recap.getThumbnailUrl()
            );
            log.info("✅ Đã tạo Recap thành công cho Recap ID: {}", recap.getId());

        } catch (Exception e) {
            log.error("❌ Lỗi render Recap ID: {}", recap.getId(), e);
            recap.setStatus(RecapStatus.FAILED);
            recapRepository.save(recap);
        }
    }
}