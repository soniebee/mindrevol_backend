package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppleLoginRequest {
    @NotBlank(message = "Identity Token must not be blank")
    private String identityToken;

    private String user; 
}