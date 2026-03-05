package com.mindrevol.core.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO đơn giản của User để trả về cho FE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;

    private String email;

    private String handle;

    private String fullname;

    private String avatarUrl;

    private String bio;

    private LocalDate dateOfBirth;

    private String gender;

    private String accountType;

    private String timezone;
}


