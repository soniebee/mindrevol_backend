package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token must not be blank")
    private String token;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}