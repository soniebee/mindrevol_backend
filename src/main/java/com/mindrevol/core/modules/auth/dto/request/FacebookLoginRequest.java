package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacebookLoginRequest {
    @NotBlank(message = "Access Token must not be blank")
    private String accessToken;
}