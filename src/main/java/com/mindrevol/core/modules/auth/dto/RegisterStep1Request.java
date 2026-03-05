package com.mindrevol.core.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bước 1: Nhập Email và Handle (Username)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep1Request {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotBlank(message = "Handle không được để trống")
    @Size(min = 3, max = 50, message = "Handle phải từ 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Handle chỉ chứa chữ cái, số và dấu gạch dưới")
    private String handle;
}

