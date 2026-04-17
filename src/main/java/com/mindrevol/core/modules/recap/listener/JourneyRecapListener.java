package com.mindrevol.core.modules.recap.listener;

import com.mindrevol.core.common.constant.AppConstants;
import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.journey.event.JourneyCompletedEvent;
import com.mindrevol.core.modules.journey.repository.JourneyRepository;
import com.mindrevol.core.modules.recap.dto.RecapTask;
import com.mindrevol.core.modules.recap.entity.Recap;
import com.mindrevol.core.modules.recap.entity.RecapStatus;
import com.mindrevol.core.modules.recap.repository.RecapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JourneyRecapListener {

    private final RecapRepository recapRepository;
    private final JourneyRepository journeyRepository;
    private final AsyncTaskProducer asyncTaskProducer;

    @Async
    @EventListener
    public void handleJourneyCompletedEvent(JourneyCompletedEvent event) {
        log.info("Bắt đầu xử lý Recap cho Journey ID: {}", event.getJourneyId());

        Optional<Journey> journeyOpt = journeyRepository.findById(event.getJourneyId());
        if (journeyOpt.isEmpty()) return;

        Journey journey = journeyOpt.get();

        // 1. Tạo Record Recap trong database với trạng thái PENDING
        Recap recap = Recap.builder()
                .userId(journey.getCreator().getId()) // ĐÃ SỬA: Lấy từ Creator
                .journeyId(journey.getId())
                .status(RecapStatus.PENDING)
                .build();
        
        recap = recapRepository.save(recap);

        // 2. Tạo Task và ném vào Queue
        RecapTask task = RecapTask.builder()
                .recapId(recap.getId())
                .journeyId(journey.getId())
                .userId(journey.getCreator().getId()) // ĐÃ SỬA: Lấy từ Creator
                .build();

        // ĐÃ SỬA: Gọi đúng hàm của bạn
        asyncTaskProducer.submitRecapTask(task);
        log.info("Đã đẩy RecapTask (ID: {}) vào hàng đợi {}", recap.getId(), AppConstants.QUEUE_RECAP_GENERATION);
    }
}