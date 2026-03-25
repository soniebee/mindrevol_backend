package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorVerifyEmailRequest {

    @Schema(description = "Email verification token sent to the user's email", example = "abc123def456...")
    @NotBlank(message = "Token must not be blank")
    private String token;
}

