package com.mindrevol.core.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response when confirming/enabling a new 2FA method
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ConfirmTwoFactorMethodResponse {

    @Schema(description = "Method type that was confirmed", example = "TOTP")
    private String method;

    @Schema(description = "Whether the method is now enabled", example = "true")
    private boolean enabled;

    @Schema(description = "Timestamp when method was enabled", example = "2026-03-23T15:30:00")
    private LocalDateTime enabledAt;

    @Schema(description = "Success message", example = "TOTP method successfully enabled")
    private String message;

    @Schema(description = "Whether backup codes need to be generated", example = "true")
    private boolean backupCodesRequired;
}
