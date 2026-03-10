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

    @Size(min = 1, max = 100, message = "Tên đầy đủ phải có độ dài từ 1 đến 100 ký tự")
    private String fullname;

    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Bio tối đa 500 ký tự")
    private String bio;

    @Size(max = 255, message = "URL website tối đa 255 ký tự")
    private String website;

    private String timezone;

    @Size(max = 500, message = "Avatar URL tối đa 500 ký tự")
    private String avatarUrl;
}

