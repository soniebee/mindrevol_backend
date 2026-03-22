package com.mindrevol.core.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckHandleRequest {
    
    @NotBlank(message = "Handle must not be blank")
    @Size(min = 3, max = 30, message = "Handle must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Handle can only contain letters, numbers, dots, and underscores")
    private String handle;
}