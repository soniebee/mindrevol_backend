package com.mindrevol.core.modules.tutorial.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.modules.tutorial.dto.TutorialStatusResponse;
import com.mindrevol.core.modules.tutorial.service.TutorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutorials")
@RequiredArgsConstructor
public class TutorialController {

    private final TutorialService tutorialService;

    // API: Kiểm tra trạng thái hướng dẫn khi vừa mở App
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TutorialStatusResponse>> getMyTutorialStatus() {
        TutorialStatusResponse status = tutorialService.getTutorialStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // API: Bắn lên khi người dùng bấm "Hoàn tất/Bỏ qua" để lưu xuống DB
    @PatchMapping("/me/complete")
    public ResponseEntity<ApiResponse<TutorialStatusResponse>> completeTutorial() {
        TutorialStatusResponse status = tutorialService.markAsCompleted();
        return ResponseEntity.ok(ApiResponse.success(status, "Đã lưu trạng thái hoàn thành hướng dẫn."));
    }
}