package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho bước 1 của Registration Wizard
 * Nhập: Email, Password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep1Dto {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 50, message = "Mật khẩu phải từ 8-50 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}

