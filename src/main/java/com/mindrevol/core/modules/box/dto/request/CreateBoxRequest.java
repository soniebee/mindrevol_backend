package com.mindrevol.core.modules.box.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateBoxRequest {

    @NotBlank(message = "Tên Box không được để trống")
    @Size(max = 100, message = "Tên Box không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @NotBlank(message = "Vui lòng chọn một Theme")
    private String themeSlug;

    private String avatar;

    private String textPosition; // Vị trí do FE truyền lên

    // Danh sách ID của bạn bè muốn mời ngay lúc tạo
    private List<String> inviteUserIds;
}