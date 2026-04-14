package com.mindrevol.core.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    @Builder.Default
    private boolean requiresTwoFactor = false;
    private String challengeId;
    private String message;

    // Available methods: ["TOTP", "EMAIL", "BACKUP_CODES"]
    private List<String> twoFactorMethods;
}