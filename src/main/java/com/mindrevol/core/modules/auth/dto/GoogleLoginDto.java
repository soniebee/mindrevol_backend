package com.mindrevol.core.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận idToken từ Frontend sau khi Google Sign In
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginDto {

    private String idToken;  // JWT từ Google Identity SDK
}

