package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorLoginVerifyRequest {

    @NotBlank(message = "Challenge id must not be blank")
    private String challengeId;

    private String otpCode;
    private String backupCode;

    @Size(max = 255, message = "deviceId must be <= 255 chars")
    private String deviceId;

    private Boolean rememberDevice;
}

