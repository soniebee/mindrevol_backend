package com.mindrevol.core.modules.recap.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateGlobalRecapRequest {
    private List<String> journeyIds; // Nhận mảng các ID Hành trình cần ghép
    private Integer speedDelayMs;
    private String filterType; // "ALL" hoặc "ME"
    private List<String> selectedCheckinIds; // Dành cho trường hợp người dùng tích chọn tay
}