package com.mindrevol.core.modules.box.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoxResponse {
    private String id;
    private String name;
    private String avatar;
    private String themeSlug;
    private long memberCount;
    private List<String> previewMemberAvatars; // Thêm trường này để làm hiệu ứng Avatar Stack
    private LocalDateTime lastActivityAt;
}