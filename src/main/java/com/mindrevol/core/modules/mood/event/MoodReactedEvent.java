package com.mindrevol.core.modules.mood.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodReactedEvent {
    private String moodId;          // ID của trạng thái đang bị thả tim
    private String boxId;           // Box chứa trạng thái này
    private String reactorId;       // Người vừa bấm nút thả tim (Người đi bão)
    private String moodOwnerId;     // Chủ nhân của trạng thái (Người hứng bão)
    private String emoji;           // Icon vừa thả (VD: ❤️, 🔥)
}