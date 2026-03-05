package com.mindrevol.core.modules.box.mapper;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.entity.Box;
import org.springframework.stereotype.Component;

@Component
public class BoxMapper {

    public Box toEntity(CreateBoxRequest request) {
        return Box.builder()
                .name(request.getName())
                .description(request.getDescription())
                .themeSlug(request.getThemeSlug())
                .avatar(request.getAvatar())
                .build();
    }

    public BoxResponse toResponse(Box box, long memberCount) {
        return BoxResponse.builder()
                // Thêm .toString() ở đây để chuyển UUID thành String
                .id(box.getId() != null ? box.getId().toString() : null)
                .name(box.getName())
                .avatar(box.getAvatar())
                .themeSlug(box.getThemeSlug())
                .memberCount(memberCount)
                .lastActivityAt(box.getLastActivityAt())
                .build();
    }

    public BoxDetailResponse toDetailResponse(Box box, long memberCount, String myRole) {
        return BoxDetailResponse.builder()
                // Thêm .toString() ở đây
                .id(box.getId() != null ? box.getId().toString() : null)
                .name(box.getName())
                .description(box.getDescription())
                .themeSlug(box.getThemeSlug())
                .avatar(box.getAvatar())
                .memberCount(memberCount)
                .myRole(myRole)
                .build();
    }
}