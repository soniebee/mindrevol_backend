package com.mindrevol.core.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorEnableRequest {

    @Schema(description = "6-digit TOTP code from authenticator app", example = "123456")
    @NotBlank(message = "OTP code must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;
}

