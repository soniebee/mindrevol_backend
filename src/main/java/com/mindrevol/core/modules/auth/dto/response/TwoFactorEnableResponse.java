package com.mindrevol.core.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TwoFactorEnableResponse {

    @Schema(description = "Two-factor enabled state after verification", example = "true")
    private boolean enabled;

    @Schema(description = "Timestamp when 2FA was enabled", example = "2026-03-21T18:55:30")
    private LocalDateTime enabledAt;
}


