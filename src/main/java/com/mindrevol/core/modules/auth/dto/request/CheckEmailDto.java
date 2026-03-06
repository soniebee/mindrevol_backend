package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để check email đã tồn tại hay chưa (với debounce)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckEmailDto {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}

