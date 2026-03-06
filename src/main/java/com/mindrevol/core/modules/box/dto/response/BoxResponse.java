package com.mindrevol.core.modules.box.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID; // Thêm import này

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxResponse {
    private UUID id; // Đã đổi từ String sang UUID
    private String name;
    private String avatar;
    private String themeSlug;
    private long memberCount;
    private LocalDateTime lastActivityAt;
}