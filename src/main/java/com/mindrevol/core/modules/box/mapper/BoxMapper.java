package com.mindrevol.core.modules.box.mapper;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.entity.Box;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// componentModel = "spring" giúp bạn @Autowired hoặc private final BoxMapper boxMapper; được luôn
@Mapper(componentModel = "spring")
public interface BoxMapper {

    // Tự động map từ Request sang Entity
    Box toEntity(CreateBoxRequest request);

    // Tự động map từ Entity sang Response, gộp thêm memberCount
    @Mapping(target = "id", source = "box.id")
    @Mapping(target = "memberCount", source = "memberCount")
    BoxResponse toResponse(Box box, long memberCount);

    // Tự động map từ Entity sang DetailResponse, gộp thêm memberCount và myRole
    @Mapping(target = "id", source = "box.id")
    @Mapping(target = "memberCount", source = "memberCount")
    @Mapping(target = "myRole", source = "myRole")
    BoxDetailResponse toDetailResponse(Box box, long memberCount, String myRole);
}