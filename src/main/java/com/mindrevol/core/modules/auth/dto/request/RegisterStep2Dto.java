package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho bước 2 của Registration Wizard
 * Nhập: Handle, Full name
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep2Dto {

    @NotBlank(message = "Handle không được để trống")
    @Size(min = 3, max = 30, message = "Handle phải từ 3-30 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Handle chỉ chứa chữ cái, số, dấu gạch dưới và gạch ngang")
    private String handle;

    @NotBlank(message = "Tên đầy đủ không được để trống")
    @Size(min = 2, max = 100, message = "Tên đầy đủ phải từ 2-100 ký tự")
    private String fullname;
}

