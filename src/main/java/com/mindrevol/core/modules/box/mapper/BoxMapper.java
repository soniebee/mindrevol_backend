package com.mindrevol.core.modules.box.mapper;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.entity.Box;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BoxMapper {

    Box toEntity(CreateBoxRequest request);

    @Mapping(target = "id", source = "box.id")
    @Mapping(target = "memberCount", source = "memberCount")
    @Mapping(target = "previewMemberAvatars", source = "previewMemberAvatars")
    BoxResponse toResponse(Box box, long memberCount, List<String> previewMemberAvatars);

    @Mapping(target = "id", source = "box.id")
    @Mapping(target = "memberCount", source = "memberCount")
    @Mapping(target = "myRole", source = "myRole")
    BoxDetailResponse toDetailResponse(Box box, long memberCount, String myRole);
}