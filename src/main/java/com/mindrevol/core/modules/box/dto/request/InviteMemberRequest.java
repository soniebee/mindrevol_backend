package com.mindrevol.core.modules.box.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank(message = "ID của người được mời không được để trống")
    private String inviteeId;
}