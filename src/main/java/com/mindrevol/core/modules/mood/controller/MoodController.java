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

    // 1. Đăng/Cập nhật Mood trong Box
    @PostMapping
    public ApiResponse<MoodResponse> createOrUpdateMood(
            @PathVariable String boxId,
            @Valid @RequestBody MoodRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        // Đã sửa thứ tự: boxId -> userId -> request
        return ApiResponse.success(moodService.createOrUpdateMood(boxId, userId, request));
    }

    // 2. Lấy toàn bộ Mood của các thành viên trong Box
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

    // 4. Thả tim bão táp (Spam thoải mái)
    @PostMapping("/{moodId}/reactions")
    public ApiResponse<Void> reactToMood(
            @PathVariable String boxId,
            @PathVariable String moodId,
            @RequestParam String type) { // type ở đây đóng vai trò là emoji
        String userId = SecurityUtils.getCurrentUserId();
        // Đã sửa thứ tự: boxId -> moodId -> userId -> type (emoji)
        moodService.reactToMood(boxId, moodId, userId, type);
        return ApiResponse.success(null);
    }

    // 5. 🔥 MỚI: Rút lại cảm xúc (Hủy thả tim)
    @DeleteMapping("/{moodId}/reactions")
    public ApiResponse<Void> removeReaction(
            @PathVariable String boxId, // Box ID vẫn nằm trên URL nhưng không dùng ở Service
            @PathVariable String moodId) {
        String userId = SecurityUtils.getCurrentUserId();
        moodService.removeReaction(moodId, userId);
        return ApiResponse.success(null);
    }
}