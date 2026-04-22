package com.mindrevol.core.modules.recap.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.recap.dto.request.CreateGlobalRecapRequest;
import com.mindrevol.core.modules.recap.dto.request.CreateRecapRequest;
import com.mindrevol.core.modules.recap.entity.Recap;
import com.mindrevol.core.modules.recap.service.RecapService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recaps")
@RequiredArgsConstructor
public class RecapController {

    private final RecapService recapService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Recap>>> getMyRecaps() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(recapService.getMyRecaps(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Recap>> getRecapDetail(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(recapService.getRecapById(id, userId)));
    }

    @PostMapping("/journey/{journeyId}")
    public ResponseEntity<ApiResponse<Void>> requestRecap(
            @PathVariable String journeyId, 
            @RequestBody CreateRecapRequest request) { 
            
        String userId = SecurityUtils.getCurrentUserId();
        recapService.requestManualRecap(journeyId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu tạo video thành công."));
    }

    @PostMapping(value = "/journey/{journeyId}/preview", produces = "video/mp4")
    public ResponseEntity<Resource> previewRecap(
            @PathVariable String journeyId,
            @RequestBody CreateRecapRequest request) {
        
        String userId = SecurityUtils.getCurrentUserId();
        byte[] videoBytes = recapService.generatePreview(journeyId, userId, request);
        
        ByteArrayResource resource = new ByteArrayResource(videoBytes);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recap_preview.mp4\"")
                .contentLength(videoBytes.length)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    // =========================================================================
    // [THÊM MỚI] ENDPOINT PREVIEW CHO NHIỀU HÀNH TRÌNH CÙNG LÚC (GLOBAL)
    // =========================================================================
    @PostMapping(value = "/global-preview", produces = "video/mp4")
    public ResponseEntity<Resource> globalPreviewRecap(@RequestBody CreateGlobalRecapRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        byte[] videoBytes = recapService.generateGlobalPreview(userId, request);
        
        ByteArrayResource resource = new ByteArrayResource(videoBytes);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"global_recap.mp4\"")
                .contentLength(videoBytes.length)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecap(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        recapService.deleteRecap(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa video."));
    }
}