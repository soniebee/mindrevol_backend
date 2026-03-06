package com.mindrevol.core.modules.box.dto.response;

import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import lombok.*;
import java.util.List;
import java.util.UUID; // Thêm import này

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxDetailResponse {

    // 1. Thông tin cơ bản của Box (Header)
    private UUID id; // Đã đổi từ String sang UUID
    private String name;
    private String description;
    private String themeSlug;
    private String avatar;
    private long memberCount;
    private String myRole;

    // 2. Chỗ chừa sẵn cho tính năng Sprint sau
    private Object mapData;
    private Object moodBubbleData;

    // 3. Danh sách hành trình
    private List<JourneyResponse> ongoingJourneys;
    private List<JourneyResponse> endedJourneys;
}