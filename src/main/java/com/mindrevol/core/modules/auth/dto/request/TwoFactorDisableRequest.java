package com.mindrevol.core.modules.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorDisableRequest {

    private String otpCode;
    private String backupCode;
}
