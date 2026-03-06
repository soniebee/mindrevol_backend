package com.mindrevol.core.modules.box.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxResponse {
    private String id; // Trở về String
    private String name;
    private String avatar;
    private String themeSlug;
    private long memberCount;
    private LocalDateTime lastActivityAt;
}