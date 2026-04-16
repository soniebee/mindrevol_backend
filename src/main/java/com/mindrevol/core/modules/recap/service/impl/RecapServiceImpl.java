package com.mindrevol.core.modules.recap.service.impl;

import com.mindrevol.core.common.constant.AppConstants;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.service.AsyncTaskProducer;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.recap.dto.RecapTask;
import com.mindrevol.core.modules.recap.dto.request.CreateGlobalRecapRequest;
import com.mindrevol.core.modules.recap.dto.request.CreateRecapRequest;
import com.mindrevol.core.modules.recap.entity.Recap;
import com.mindrevol.core.modules.recap.entity.RecapStatus;
import com.mindrevol.core.modules.recap.repository.RecapRepository;
import com.mindrevol.core.modules.recap.service.RecapService;
import com.mindrevol.core.modules.recap.service.strategy.FFmpegRecapGeneratorImpl;
import com.mindrevol.core.modules.recap.service.strategy.RecapGeneratorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecapServiceImpl implements RecapService {

    private final RecapRepository recapRepository;
    private final AsyncTaskProducer asyncTaskProducer;
    private final CheckinRepository checkinRepository;
    private final RecapGeneratorStrategy recapGenerator;

    @Override
    public List<Recap> getMyRecaps(String userId) {
        return recapRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Recap getRecapById(String recapId, String userId) {
        Recap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video nhìn lại"));
        
        if (!recap.getUserId().equals(userId)) {
            throw new BadRequestException("Không có quyền truy cập");
        }
        return recap;
    }

    @Override
    public void requestManualRecap(String journeyId, String userId, CreateRecapRequest request) {
        Recap existing = recapRepository.findFirstByJourneyIdOrderByCreatedAtDesc(journeyId).orElse(null);
        if (existing != null && (existing.getStatus() == RecapStatus.PENDING || existing.getStatus() == RecapStatus.PROCESSING)) {
            throw new BadRequestException("Video của hành trình này đang được xử lý. Vui lòng chờ.");
        }

        Recap recap = Recap.builder()
                .userId(userId)
                .journeyId(journeyId)
                .status(RecapStatus.PENDING)
                .build();
        recap = recapRepository.save(recap);

        RecapTask task = RecapTask.builder()
                .recapId(recap.getId())
                .journeyId(journeyId)
                .userId(userId)
                .speedDelayMs(request.getSpeedDelayMs() != null ? request.getSpeedDelayMs() : 500)
                .filterType(request.getFilterType() != null ? request.getFilterType() : "ALL")
                .selectedCheckinIds(request.getSelectedCheckinIds())
                .build();
                
        asyncTaskProducer.submitRecapTask(task);
    }

    @Override
    public void deleteRecap(String recapId, String userId) {
        Recap recap = getRecapById(recapId, userId);
        recapRepository.delete(recap);
    }

    @Override
    public byte[] generatePreview(String journeyId, String userId, CreateRecapRequest request) {
        List<Checkin> checkins;
        if (request.getSelectedCheckinIds() != null && !request.getSelectedCheckinIds().isEmpty()) {
            checkins = checkinRepository.findAllById(request.getSelectedCheckinIds());
        } else if ("ME".equals(request.getFilterType())) {
            checkins = checkinRepository.findMediaByJourneyIdAndUserId(journeyId, userId);
        } else {
            checkins = checkinRepository.findMediaByJourneyIdForRecap(journeyId);
        }

        List<String> imageUrls = checkins.stream()
                .map(Checkin::getImageUrl)
                .filter(url -> url != null && !url.isEmpty())
                .collect(Collectors.toList());

        if (imageUrls.isEmpty()) {
            throw new BadRequestException("Không tìm thấy ảnh để tạo video.");
        }

        try {
            int delayMs = request.getSpeedDelayMs() != null ? request.getSpeedDelayMs() : 500;
            return ((FFmpegRecapGeneratorImpl) recapGenerator).generatePreviewVideo(imageUrls, delayMs);
        } catch (Exception e) {
            log.error("Lỗi tạo preview video", e);
            throw new BadRequestException("Không thể tạo video xem trước: " + e.getMessage());
        }
    }

    // =========================================================================
    // [THÊM MỚI] RENDER PREVIEW CHO NHIỀU HÀNH TRÌNH TỔNG HỢP (GLOBAL)
    // =========================================================================
    @Override
    public byte[] generateGlobalPreview(String userId, CreateGlobalRecapRequest request) {
        if (request.getJourneyIds() == null || request.getJourneyIds().isEmpty()) {
             throw new BadRequestException("Vui lòng chọn ít nhất 1 hành trình.");
        }

        List<Checkin> checkins;
        
        // Nếu người dùng chọn đích danh từng ảnh từ UI
        if (request.getSelectedCheckinIds() != null && !request.getSelectedCheckinIds().isEmpty()) {
            checkins = checkinRepository.findAllById(request.getSelectedCheckinIds());
        } 
        // Lọc theo chế độ "Chỉ mình tôi" trong các hành trình đã chọn
        else if ("ME".equals(request.getFilterType())) {
            checkins = checkinRepository.findMediaByMultipleJourneyIdsAndUserId(request.getJourneyIds(), userId);
        } 
        // Lấy tất cả ảnh từ các hành trình đã chọn
        else {
            checkins = checkinRepository.findMediaByMultipleJourneyIds(request.getJourneyIds());
        }

        List<String> imageUrls = checkins.stream()
                .map(Checkin::getImageUrl)
                .filter(url -> url != null && !url.isEmpty())
                .collect(Collectors.toList());

        if (imageUrls.isEmpty()) {
            throw new BadRequestException("Không tìm thấy ảnh để tạo video trong các hành trình đã chọn.");
        }

        try {
            int delayMs = request.getSpeedDelayMs() != null ? request.getSpeedDelayMs() : 500;
            return ((FFmpegRecapGeneratorImpl) recapGenerator).generatePreviewVideo(imageUrls, delayMs);
        } catch (Exception e) {
            log.error("Lỗi tạo global preview video", e);
            throw new BadRequestException("Không thể tạo video tổng hợp: " + e.getMessage());
        }
    }
}