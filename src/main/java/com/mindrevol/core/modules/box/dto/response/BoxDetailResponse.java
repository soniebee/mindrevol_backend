package com.mindrevol.core.modules.box.dto.response;

import com.mindrevol.core.modules.journey.dto.response.JourneyResponse; // Import DTO của module Journey
import lombok.*;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxDetailResponse {

    // 1. Thông tin cơ bản của Box (Header)
    private String id;
    private String name;
    private String description;
    private String themeSlug;
    private String avatar;
    private long memberCount;
    private String myRole; // Trả về "ADMIN" hoặc "MEMBER" để App biết có được hiện nút Settings/Xóa Box không

    // 2. Chỗ chừa sẵn cho tính năng Sprint sau (Map & Mood Bubble)
    // Hiện tại BE sẽ trả về null hoặc object rỗng, Mobile cứ thấy null là tự hiểu chưa có data
    private Object mapData;
    private Object moodBubbleData;

    // 3. Danh sách hành trình (Chia làm 2 list rõ ràng theo UI của bạn)
    private List<JourneyResponse> ongoingJourneys; // Hành trình đang hoạt động
    private List<JourneyResponse> endedJourneys;   // Hành trình đã kết thúc (Memories)
}