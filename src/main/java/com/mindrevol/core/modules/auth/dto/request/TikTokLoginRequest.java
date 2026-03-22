package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TikTokLoginRequest {
    @NotBlank(message = "Auth code must not be blank")
    private String code;

    @NotBlank(message = "Code verifier must not be blank")
    private String codeVerifier; // <--- THÊM
}