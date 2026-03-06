package com.mindrevol.core.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin user sau khi xác thực với Social Provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo {

    private String email;
    private String name;
    private String picture;
    private String provider;        // "google", "apple", "facebook", etc.
    private String providerId;      // ID từ provider (Google Sub, Apple ID, etc.)
}

