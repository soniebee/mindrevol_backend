package com.mindrevol.core.modules.journey.dto.response;

import lombok.*;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class JourneyResponse {

    private String id; // ID của hành trình (để String cho DTO)

    private String name; // Tên hành trình

    private String thumbnail; // Hình thu nhỏ hoặc màu nền của hành trình

    private String status; // Trạng thái: "ONGOING" (Đang chạy) hoặc "ENDED" (Đã kết thúc)

    // Bạn có thể thêm các trường khác sau này khi làm module Journey
}