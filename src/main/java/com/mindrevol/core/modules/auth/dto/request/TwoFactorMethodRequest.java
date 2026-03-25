package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorMethodRequest {

    @Schema(description = "2FA method type", example = "TOTP", allowableValues = {"TOTP", "EMAIL"})
    @NotBlank(message = "Method must not be blank")
    private String method; // "TOTP" or "EMAIL"

    @Schema(description = "Email address for 2FA code delivery (required if method=EMAIL)", example = "user@example.com")
    @Email(message = "Email must be valid")
    private String email; // Required if method=EMAIL
}

