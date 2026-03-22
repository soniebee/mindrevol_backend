package com.mindrevol.core.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO để cập nhật thông tin hồ sơ người dùng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileDto {

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullname;

    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    @Size(max = 255, message = "Website URL must be at most 255 characters")
    private String website;

    private String timezone;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;
}

