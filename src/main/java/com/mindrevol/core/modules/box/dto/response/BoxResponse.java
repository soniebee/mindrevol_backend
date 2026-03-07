package com.mindrevol.core.modules.box.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxResponse {
    private String id;
    private String name;
    private String avatar;
    private String themeSlug; // Dùng để load khung ảnh của bạn
    private long memberCount; // Để App biết là Solo hay Group mà ẩn/hiện nút Chat
    private LocalDateTime lastActivityAt;
}