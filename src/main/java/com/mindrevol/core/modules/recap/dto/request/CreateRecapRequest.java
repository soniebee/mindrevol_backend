package com.mindrevol.core.modules.recap.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateRecapRequest {
    private Integer speedDelayMs; // VD: 120 (rất nhanh), 500 (vừa), 1500 (chậm)
    private String filterType;    // "ALL" (Tất cả) hoặc "ME" (Chỉ mình tôi)
    private List<String> selectedCheckinIds; // Danh sách ID ảnh do user tự chọn (nếu có)
}