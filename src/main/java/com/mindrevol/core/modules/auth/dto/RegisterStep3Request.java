package com.mindrevol.core.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bước 3: Xác thực OTP
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep3Request {

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải có 6 ký tự")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP chỉ chứa số")
    private String otpCode;
}

