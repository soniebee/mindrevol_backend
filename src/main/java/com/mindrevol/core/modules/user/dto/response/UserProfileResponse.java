package com.mindrevol.core.modules.user.dto.response;

import com.mindrevol.core.modules.user.entity.AccountType;
import com.mindrevol.core.modules.user.entity.Gender;
import com.mindrevol.core.modules.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO Response cho thông tin hồ sơ người dùng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String id;
    private String email;
    private String handle;
    private String fullname;
    private LocalDate dateOfBirth;
    private String bio;
    private String website;
    private String avatarUrl;
    private String timezone;
    private Gender gender;
    private String authProvider;
    private UserStatus status;
    private AccountType accountType;
    private LocalDateTime subscriptionExpiryDate;
    private Long points;
    private boolean isPremium;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

