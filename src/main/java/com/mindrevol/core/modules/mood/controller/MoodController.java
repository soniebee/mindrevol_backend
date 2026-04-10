package com.mindrevol.core.modules.mood.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;
import com.mindrevol.core.modules.mood.service.MoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boxes/{boxId}/moods")
@RequiredArgsConstructor
public class MoodController {

    private final MoodService moodService;

    // 1. Đăng/Cập nhật Mood
    @PostMapping
    public ApiResponse<MoodResponse> createOrUpdateMood(
            @PathVariable String boxId,
            @Valid @RequestBody MoodRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(moodService.createOrUpdateMood(boxId, userId, request));
    }

    // 2. Lấy danh sách Mood
    @GetMapping
    public ApiResponse<List<MoodResponse>> getBoxMoods(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(moodService.getActiveMoodsInBox(boxId, userId));
    }

    // 3. Tự xóa Mood của mình
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyMood(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        moodService.deleteMyMood(boxId, userId);
        return ApiResponse.success(null);
    }

    // 4. Thả cảm xúc (Thả tim)
    @PostMapping("/{moodId}/reactions")
    public ApiResponse<Void> reactToMood(
            @PathVariable String boxId,
            @PathVariable String moodId,
            @RequestParam String type) { 
        String userId = SecurityUtils.getCurrentUserId();
        moodService.reactToMood(boxId, moodId, userId, type);
        return ApiResponse.success(null);
    }

    // 5. Gỡ bỏ cảm xúc đã thả
    @DeleteMapping("/{moodId}/reactions")
    public ApiResponse<Void> removeReaction(
            @PathVariable String boxId, 
            @PathVariable String moodId) {
        String userId = SecurityUtils.getCurrentUserId();
        moodService.removeReaction(moodId, userId);
        return ApiResponse.success(null);
    }

    // 6. 🔥 TÍNH NĂNG MỚI: Hỏi thăm/Chọc bạn bè
    @PostMapping("/ask/{targetUserId}")
    public ApiResponse<Void> askFriendMood(
            @PathVariable String boxId,
            @PathVariable String targetUserId) {
        String askerId = SecurityUtils.getCurrentUserId();
        moodService.askFriendMood(boxId, askerId, targetUserId);
        return ApiResponse.success(null);
    }
}