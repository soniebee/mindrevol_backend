package com.mindrevol.core.modules.checkin.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.core.modules.checkin.dto.request.ReactionRequest;
import com.mindrevol.core.modules.checkin.dto.request.UpdateCheckinRequest;
import com.mindrevol.core.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.core.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.core.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.core.modules.checkin.dto.response.MapMarkerResponse; 
import com.mindrevol.core.modules.checkin.service.CheckinService;
import com.mindrevol.core.modules.checkin.service.ReactionService;
import com.mindrevol.core.modules.feed.dto.FeedItemResponse;
import com.mindrevol.core.modules.feed.service.FeedService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final UserService userService;
    private final ReactionService reactionService;
    private final FeedService feedService;

    // --- MAIN FEED ---

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<CheckinResponse>> createCheckin(@ModelAttribute @Valid CheckinRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        CheckinResponse response = checkinService.createCheckin(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/journeys/grid")
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getJourneyGridFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "18") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(feedService.getJourneyGridFeed(userId, page, limit)));
    }

    @GetMapping("/me/archived")
    public ResponseEntity<ApiResponse<Page<CheckinResponse>>> getArchivedCheckins(Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(checkinService.getArchivedCheckins(currentUser, pageable)));
    }

    @GetMapping("/unified")
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getUnifiedFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        int offset = page * limit; 
        return ResponseEntity.ok(ApiResponse.success(feedService.getNewsFeed(userId, offset, limit)));
    }

    @GetMapping("/journey/{journeyId}")
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getJourneyFeed(
            @PathVariable String journeyId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int limit) {
        
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        
        List<CheckinResponse> checkins = checkinService.getJourneyFeedByCursor(journeyId, currentUser, cursor, limit);
        
        List<FeedItemResponse> feed = new ArrayList<>(checkins);

        if (!currentUser.isPremium() && !feed.isEmpty()) {
            feed = feedService.injectContextualAds(feed, 0, limit);
        }

        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    // --- INTERACTIONS ---

    @PostMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> postComment(
            @PathVariable String checkinId,
            @RequestBody String content) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(checkinService.postComment(checkinId, content, currentUser)));
    }

    @GetMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable String checkinId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(checkinService.getComments(checkinId, pageable)));
    }

    @PostMapping("/{checkinId}/reactions")
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @PathVariable String checkinId,
            @RequestBody @Valid ReactionRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        reactionService.toggleReaction(checkinId, userId, request.getEmoji(), request.getMediaUrl());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{checkinId}/reactions")
    public ResponseEntity<ApiResponse<List<CheckinReactionDetailResponse>>> getReactions(@PathVariable String checkinId) {
        return ResponseEntity.ok(ApiResponse.success(reactionService.getReactions(checkinId)));
    }

    // --- ACTIONS ---
    @PutMapping("/{checkinId}")
    public ResponseEntity<ApiResponse<CheckinResponse>> updateCheckin(
            @PathVariable String checkinId,
            @RequestBody UpdateCheckinRequest request) { 
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(
            checkinService.updateCheckin(checkinId, request, currentUser)
        ));
    }

    @DeleteMapping("/{checkinId}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckin(@PathVariable String checkinId) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        checkinService.deleteCheckin(checkinId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa bài viết thành công"));
    }

    // --- MAPS ---
    @GetMapping("/map/journey/{journeyId}")
    public ResponseEntity<ApiResponse<List<MapMarkerResponse>>> getMapMarkersForJourney(@PathVariable String journeyId) {
        User currentUser = (User) SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(checkinService.getMapMarkersForJourney(journeyId, currentUser)));
    }

    @GetMapping("/map/box/{boxId}")
    public ResponseEntity<ApiResponse<List<MapMarkerResponse>>> getMapMarkersForBox(@PathVariable String boxId) {
        User currentUser = (User) SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(checkinService.getMapMarkersForBox(boxId, currentUser)));
    }
    
    @GetMapping("/map/me")
    public ResponseEntity<ApiResponse<List<MapMarkerResponse>>> getMyMapMarkers() {
        User currentUser = (User) SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(checkinService.getMyMapMarkers(currentUser)));
    }

    // =========================================================================
    // [THÊM MỚI] API lấy toàn bộ ảnh của hành trình để làm Recap
    // =========================================================================
    @GetMapping("/journey/{journeyId}/photos")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getJourneyPhotos(@PathVariable String journeyId) {
        List<CheckinResponse> photos = checkinService.getJourneyPhotosForRecap(journeyId);
        return ResponseEntity.ok(ApiResponse.success(photos, "Lấy danh sách ảnh hành trình thành công"));
    }
    
    @PostMapping("/journeys/photos/batch")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getPhotosFromMultipleJourneys(@RequestBody List<String> journeyIds) {
        List<CheckinResponse> photos = checkinService.getMultipleJourneysPhotosForRecap(journeyIds);
        return ResponseEntity.ok(ApiResponse.success(photos, "Lấy danh sách ảnh tổng hợp thành công"));
    }
}