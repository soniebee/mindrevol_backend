package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendRegisterOtpRequest {
    
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;
}