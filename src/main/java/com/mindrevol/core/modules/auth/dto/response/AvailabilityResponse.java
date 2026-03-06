package com.mindrevol.core.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho các API check email/handle available
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private boolean available;  // true nếu chưa tồn tại, false nếu đã tồn tại
    private String message;     // Thông báo chi tiết
}

