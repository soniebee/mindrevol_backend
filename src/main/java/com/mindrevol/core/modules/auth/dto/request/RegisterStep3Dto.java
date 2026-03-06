package com.mindrevol.core.modules.auth.dto.request;

import com.mindrevol.core.modules.user.entity.Gender;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO cho bước 3 của Registration Wizard (Tùy chọn)
 * Nhập: Ngày sinh, Giới tính
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStep3Dto {

    @PastOrPresent(message = "Ngày sinh không được trong tương lai")
    private LocalDate dateOfBirth;

    private Gender gender;
}

