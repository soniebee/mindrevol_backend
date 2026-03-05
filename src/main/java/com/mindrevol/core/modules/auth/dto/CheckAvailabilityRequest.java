package com.mindrevol.core.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để check Email hoặc Handle có tồn tại không
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckAvailabilityRequest {

    private String email;

    private String handle;
}

