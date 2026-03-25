package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to start setup of a new 2FA method
 */
@Getter
@Setter
public class StartTwoFactorMethodSetupRequest {

    @Schema(description = "2FA method type to setup (optional, defaults to TOTP)", example = "TOTP", allowableValues = {"TOTP", "EMAIL"})
    private String method;

    @Schema(description = "Email used when method=EMAIL (optional, defaults to current account email)", example = "user@example.com")
    private String email;
}
