package com.mindrevol.core.modules.checkin.service.impl;

import com.mindrevol.core.common.constant.AppConstants;
import com.mindrevol.core.common.event.CheckinSuccessEvent;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.service.ContentModerationService;
import com.mindrevol.core.common.service.ImageMetadataService;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.core.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.core.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.core.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.core.modules.checkin.entity.*;
import com.mindrevol.core.modules.checkin.event.CheckinDeletedEvent;
import com.mindrevol.core.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.core.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.core.modules.checkin.repository.CheckinCommentRepository;
import com.mindrevol.core.modules.checkin.repository.CheckinRepository;
import com.mindrevol.core.modules.checkin.service.CheckinService;
import com.mindrevol.core.modules.checkin.service.ReactionService;
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.journey.entity.JourneyParticipant;
import com.mindrevol.core.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.core.modules.journey.repository.JourneyRepository;
import com.mindrevol.core.modules.storage.service.FileStorageService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;

    private final CheckinCommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository;

    private final ReactionService reactionService;
    private final ContentModerationService moderationService;
    private final ImageMetadataService metadataService;

    private Set<String> getExcludedUserIds(String userId) {
        Set<String> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        if (blockedIds == null) blockedIds = new HashSet<>();
        blockedIds.add("-1");
        return blockedIds;
    }

    private CheckinResponse enrichResponse(CheckinResponse response) {
        List<CheckinReactionDetailResponse> previews = reactionService.getPreviewReactions(response.getId());
        response.setLatestReactions(previews);
        return response;
    }

    @Override
    @CacheEvict(value = "journey_widget", key = "#request.journeyId + '-' + #currentUser.id")
    @Transactional
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        //Khởi tạo các biến để xử lý UNSORTED
        Journey journey = null;
        JourneyParticipant participant = null;
        LocalDateTime expiresAt = null;

        //Logic tự động xử lý khi post ảnh mà không chọn Journey
        if (request.getJourneyId() == null || request.getJourneyId().trim().isEmpty() || request.getJourneyId().equalsIgnoreCase("UNSORTED")) {
            journey = new Journey();
            journey.setId("UNSORTED");
            expiresAt = LocalDateTime.now().plusDays(14); // Set hạn sử dụng 14 ngày
            log.info("Tạo checkin UNSORTED cho user {}, hết hạn lúc: {}", currentUser.getId(), expiresAt);
        } else {
            // Logic cũ: Có journey thì kiểm tra DB
            journey = journeyRepository.findById(request.getJourneyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

            participant = participantRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId())
                    .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên"));
        }

        String tz = currentUser.getTimezone() != null ? currentUser.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate todayLocal = LocalDate.now(userZone);

        // [THÊM]: Bỏ qua check EndDate nếu đây là bài UNSORTED
        if (!journey.getId().equals("UNSORTED") && journey.getEndDate() != null && todayLocal.isAfter(journey.getEndDate().plusDays(1))) {
            throw new BadRequestException("Hành trình này đã kết thúc (Hạn chót: " + journey.getEndDate() + ").");
        }

        String imageUrl = "";
        String videoUrl = null;
        String imageFileId = null;
        MediaType mediaType = MediaType.IMAGE;

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            MultipartFile file = request.getFile();
            String contentType = file.getContentType();

            if (contentType != null && contentType.startsWith("video")) {
                if (!currentUser.isPremium()) {
                    throw new BadRequestException("Tính năng Video/Live Photo chỉ dành cho thành viên GOLD.");
                }

                if (file.getSize() > AppConstants.MAX_VIDEO_SIZE_BYTES) {
                    throw new BadRequestException("Video quá lớn. Vui lòng tải video dưới 10MB (Khoảng 3s).");
                }

                mediaType = MediaType.VIDEO;

                try {
                    FileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file, AppConstants.STORAGE_CHECKIN_VIDEOS + journey.getId());
                    imageFileId = uploadResult.getFileId();
                    String rawUrl = uploadResult.getUrl();
                    videoUrl = rawUrl + "?tr=w-720,q-60,an-true";
                    imageUrl = rawUrl + "/ik-thumbnail.jpg?tr=w-720";
                } catch (Exception e) {
                    throw new BadRequestException("Lỗi upload video: " + e.getMessage());
                }

            } else {
                mediaType = MediaType.IMAGE;
                try {
                    FileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file, AppConstants.STORAGE_CHECKIN_IMAGES + journey.getId());
                    imageUrl = uploadResult.getUrl() + "?tr=w-1080,q-80";
                    imageFileId = uploadResult.getFileId();
                    moderationService.validateImage(uploadResult.getUrl());
                } catch (BadRequestException e) {
                    if (imageFileId != null) fileStorageService.deleteFile(imageFileId);
                    throw e;
                } catch (Exception e) {
                    throw new BadRequestException("Lỗi xử lý hình ảnh: " + e.getMessage());
                }
            }
        } else {
            throw new BadRequestException("Vui lòng tải lên hình ảnh hoặc video.");
        }

        // [THÊM]: Truyền thêm expiresAt vào hàm save
        return saveCheckinTransaction(currentUser, journey, participant, request,
                imageUrl, videoUrl, imageFileId, mediaType, todayLocal, expiresAt);
    }

    @Transactional
    protected CheckinResponse saveCheckinTransaction(User currentUser, Journey journey,
                                                     JourneyParticipant participant,
                                                     CheckinRequest request,
                                                     String imageUrl,
                                                     String videoUrl,
                                                     String imageFileId,
                                                     MediaType mediaType,
                                                     LocalDate todayLocal,
                                                     LocalDateTime expiresAt) { // [THÊM]: Tham số expiresAt

        CheckinStatus finalStatus = CheckinStatus.NORMAL;
        if (request.getStatusRequest() != null && request.getStatusRequest().toString().equalsIgnoreCase("REST")) {
            finalStatus = CheckinStatus.REST;
        }

        String finalActivityName = request.getActivityName();
        if (finalActivityName != null && finalActivityName.trim().isEmpty()) {
            finalActivityName = null;
        }

        String finalEmotion = Emotion.NORMAL.name();

        if (request.getEmotion() != null) {
            finalEmotion = request.getEmotion().toString();
        }

        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .caption(request.getCaption())
                .mediaType(mediaType)
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .imageFileId(imageFileId)
                .thumbnailUrl(imageUrl)
                .emotion(finalEmotion)
                .activityType(request.getActivityType() != null ? request.getActivityType() : ActivityType.DEFAULT)
                .activityName(finalActivityName)
                .locationName(request.getLocationName())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .status(finalStatus)
                .visibility(request.getVisibility() != null ? request.getVisibility() : CheckinVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .checkinDate(todayLocal)
                .chapterId(request.getChapterId()) // [THÊM]: Lưu Chapter ID
                .expiresAt(expiresAt)              // [THÊM]: Lưu thời gian hết hạn
                .build();

        checkin = checkinRepository.save(checkin);

        updateParticipantStats(participant, finalStatus, todayLocal);

        eventPublisher.publishEvent(new CheckinSuccessEvent(
                checkin.getId(),
                currentUser.getId(),
                journey.getId(),
                checkin.getCreatedAt()
        ));

        return checkinMapper.toResponse(checkin);
    }

    private void updateParticipantStats(JourneyParticipant participant, CheckinStatus status, LocalDate todayLocal) {
        // [THÊM]: Nếu bài đăng UNSORTED (không có journey) thì bỏ qua không cần cộng streak
        if (participant == null) {
            return;
        }

        boolean isFirstCheckinToday = false;

        if (participant.getLastCheckinAt() == null) {
            isFirstCheckinToday = true;
        } else {
            LocalDate lastDate = participant.getLastCheckinAt().toLocalDate();
            if (!lastDate.equals(todayLocal)) {
                isFirstCheckinToday = true;
            }
        }

        if (isFirstCheckinToday) {
            participant.setTotalActiveDays(participant.getTotalActiveDays() + 1);
        }

        if (status == CheckinStatus.REST) {
            participant.setLastCheckinAt(LocalDateTime.now());
        } else {
            if (participant.getLastCheckinAt() == null) {
                participant.setCurrentStreak(1);
            } else {
                LocalDate lastDate = participant.getLastCheckinAt().toLocalDate();
                if (!lastDate.equals(todayLocal)) {
                    long daysGap = ChronoUnit.DAYS.between(lastDate, todayLocal);
                    if (daysGap > 1) {
                        participant.setCurrentStreak(1);
                    } else if (daysGap == 1) {
                        participant.setCurrentStreak(participant.getCurrentStreak() + 1);
                    }
                }
            }

            participant.setLastCheckinAt(LocalDateTime.now());
            participant.setTotalCheckins(participant.getTotalCheckins() + 1);
        }

        participantRepository.save(participant);
    }

    // [THÊM]: Bổ sung biến chapterId vào interface hàm để hỗ trợ filter Chapter
    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getJourneyFeed(String journeyId, String chapterId, Pageable pageable, User currentUser) {
        // [THÊM]: Bypass check quyền nếu là UNSORTED
        if (!journeyId.equals("UNSORTED") && !participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Không có quyền xem");
        }

        Page<Checkin> checkins;
        // [THÊM]: Logic lọc theo chapter
        if (chapterId != null && !chapterId.trim().isEmpty()) {
            checkins = checkinRepository.findByJourneyIdAndChapterIdOrderByCreatedAtDesc(journeyId, chapterId, pageable);
        } else {
            checkins = checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId, pageable);
        }

        return checkins.map(checkinMapper::toResponse)
                .map(this::enrichResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit) {
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> excludedUserIds = getExcludedUserIds(currentUser.getId());

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        return checkinRepository.findUnifiedFeedRecent(
                        currentUser.getId(),
                        threeDaysAgo,
                        cursor,
                        excludedUserIds,
                        pageable
                )
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }

    // [THÊM]: Bổ sung biến chapterId vào đây tương tự
    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getJourneyFeedByCursor(String journeyId, String chapterId, User currentUser, LocalDateTime cursor, int limit) {
        if (!journeyId.equals("UNSORTED") && !participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem hành trình này");
        }
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> excludedUserIds = getExcludedUserIds(currentUser.getId());

        // Ghi chú: Vì bạn đang dùng custom Query (findJourneyFeedByCursor) trong CheckinRepository
        // Nên nếu muốn lọc theo Chapter ở chế độ Cursor, bạn sẽ cần báo bạn BE viết thêm 1 hàm findJourneyFeedByChapterAndCursor vào Repo nhé.
        // Tạm thời nếu ko có chapterId, ta vẫn dùng hàm cũ của bạn:
        return checkinRepository.findJourneyFeedByCursor(journeyId, cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(this::enrichResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponse postComment(String checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (userBlockRepository.existsByBlockerIdAndBlockedId(checkin.getUser().getId(), currentUser.getId())) {
            throw new BadRequestException("Bạn không thể bình luận bài viết này.");
        }

        CheckinComment comment = CheckinComment.builder()
                .checkin(checkin)
                .user(currentUser)
                .content(content)
                .build();

        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentPostedEvent(checkin, currentUser, content));
        return checkinMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String checkinId, Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        Set<String> excludedUserIds = getExcludedUserIds(currentUserId);

        return commentRepository.findByCheckinId(checkinId, excludedUserIds, pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    @Transactional
    public CheckinResponse updateCheckin(String checkinId, String caption, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền sửa bài viết này");
        }

        checkin.setCaption(caption);
        checkin = checkinRepository.save(checkin);
        return checkinMapper.toResponse(checkin);
    }

    @Override
    @Transactional
    @CacheEvict(value = "journey_widget", allEntries = true)
    public void deleteCheckin(String checkinId, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
            if (!isAdmin) throw new BadRequestException("Bạn không có quyền xóa bài viết này");
        }

        Journey journey = checkin.getJourney();
        checkinRepository.delete(checkin);
        checkinRepository.flush();

        if (checkin.getImageFileId() != null) {
            eventPublisher.publishEvent(new CheckinDeletedEvent(checkin.getImageFileId()));
        }

        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(journey.getId(), checkin.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        recalculateParticipantStats(participant, checkin.getUser());
    }

    private void recalculateParticipantStats(JourneyParticipant participant, User user) {
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(userZone);

        List<LocalDateTime> history = checkinRepository.findValidCheckinDates(
                participant.getJourney().getId(),
                user.getId()
        );

        if (history.isEmpty()) {
            participant.setCurrentStreak(0);
            participant.setTotalCheckins(0);
            participant.setTotalActiveDays(0);
            participant.setLastCheckinAt(null);
            participantRepository.save(participant);
            return;
        }

        participant.setTotalCheckins(history.size());

        Set<LocalDate> uniqueDates = history.stream()
                .map(dt -> dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(userZone).toLocalDate())
                .collect(Collectors.toSet());
        participant.setTotalActiveDays(uniqueDates.size());

        LocalDateTime lastCheckinTime = history.get(0);
        participant.setLastCheckinAt(lastCheckinTime);

        int streak = 0;
        LocalDate cursorDate = today;

        if (!uniqueDates.contains(cursorDate)) {
            if (uniqueDates.contains(cursorDate.minusDays(1))) {
                cursorDate = cursorDate.minusDays(1);
            } else {
                participant.setCurrentStreak(0);
                participantRepository.save(participant);
                return;
            }
        }

        while (uniqueDates.contains(cursorDate)) {
            streak++;
            cursorDate = cursorDate.minusDays(1);
        }

        participant.setCurrentStreak(streak);
        participantRepository.save(participant);
    }
}