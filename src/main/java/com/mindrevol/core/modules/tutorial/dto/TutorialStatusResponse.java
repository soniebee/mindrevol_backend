package com.mindrevol.core.modules.tutorial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialStatusResponse {
    // Trả về true nếu đã xem xong, false nếu chưa xem
    private boolean isCompleted;
}