package com.mindrevol.core.modules.mood.controller;

import com.mindrevol.core.modules.mood.dto.request.MoodRequest;
import com.mindrevol.core.modules.mood.dto.response.MoodResponse;
import com.mindrevol.core.modules.mood.entity.Mood;
import com.mindrevol.core.modules.mood.service.impl.MoodServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MoodController {

    private final MoodServiceImpl moodService;

    // 1. Thêm hoặc Cập nhật Mood của mình trong Box
    @PostMapping("/boxes/{boxId}/moods")
    public ResponseEntity<MoodResponse> createOrUpdateMood(
            @PathVariable String boxId,
            @Valid @RequestBody MoodRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(moodService.createOrUpdateMood(boxId, userId, request));
    }

    // 2. Lấy danh sách Mood của tất cả mọi người trong Box
    @GetMapping("/boxes/{boxId}/moods")
    public ResponseEntity<List<MoodResponse>> getActiveMoods(@PathVariable String boxId) {
        return ResponseEntity.ok(moodService.getActiveMoodsInBox(boxId));
    }

    // 3. Thả React vào Mood của người khác
    @PostMapping("/moods/{moodId}/reactions")
    public ResponseEntity<Void> reactToMood(
            @PathVariable String moodId,
            @RequestParam String emoji,
            @RequestHeader("X-User-Id") String userId) {
        moodService.reactToMood(moodId, userId, emoji);
        return ResponseEntity.ok().build();
    }
}