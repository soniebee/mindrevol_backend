package com.mindrevol.core.modules.box.service;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoxService {
    // 1. Tạo Box mới (Đổi UUID thành String)
    BoxDetailResponse createBox(CreateBoxRequest request, String userId);

    // 2. Lấy danh sách Box cho trang chủ (Đổi UUID thành String)
    Page<BoxResponse> getMyBoxes(String userId, Pageable pageable);

    // 3. Lấy chi tiết một Box (Đổi UUID thành String)
    BoxDetailResponse getBoxDetail(String boxId, String userId);
}