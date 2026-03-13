package com.mindrevol.core.modules.box.dto.request;

import com.mindrevol.core.modules.box.entity.BoxRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    @NotNull(message = "Role không được để trống")
    private BoxRole role;
}