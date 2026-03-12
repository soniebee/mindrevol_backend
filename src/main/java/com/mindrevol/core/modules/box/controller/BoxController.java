package com.mindrevol.core.modules.box.controller;

// 1. Đảm bảo có đầy đủ các import này
import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.service.BoxService; // Quan trọng: Nhận diện BoxService
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boxes")
@RequiredArgsConstructor // Tự động inject boxService qua Constructor
public class BoxController {

    // 2. Phải có dòng khai báo này thì các hàm bên dưới mới dùng được 'boxService'
    private final BoxService boxService;

    // API Tạo Box mới
    @PostMapping
    public ResponseEntity<BoxDetailResponse> createBox(
            @Valid @RequestBody CreateBoxRequest request,
            @RequestHeader("X-User-Id") String userId) {

        BoxDetailResponse response = boxService.createBox(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // API Lấy danh sách Box cho trang chủ
    @GetMapping
    public ResponseEntity<Page<BoxResponse>> getMyBoxes(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivityAt").descending());
        Page<BoxResponse> responses = boxService.getMyBoxes(userId, pageable);
        return ResponseEntity.ok(responses);
    }

    // API Lấy chi tiết một Box
    @GetMapping("/{boxId}")
    public ResponseEntity<BoxDetailResponse> getBoxDetail(
            @PathVariable String boxId,
            @RequestHeader("X-User-Id") String userId) {

        BoxDetailResponse response = boxService.getBoxDetail(boxId, userId);
        return ResponseEntity.ok(response);
    }
} // 3. Đảm bảo có dấu ngoặc kết thúc class ở đây