package com.mindrevol.core.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupCodeStatusDto {

    @Schema(description = "Original backup code (masked with *** in some contexts)")
    private String code;

    @Schema(description = "Whether this backup code has been used", example = "false")
    private boolean used;

    @Schema(description = "Timestamp when this code was used (null if not used)", example = "2026-03-21T10:15:00")
    private LocalDateTime usedAt;

    /**
     * Generate masked version of the code for display
     * Example: ABCD3EFG7H → ABCD****H
     */
    public String getMaskedCode() {
        if (code == null || code.length() <= 4) {
            return code;
        }
        int visibleChars = 4; // Show first 4 chars
        return code.substring(0, visibleChars) + "****" + code.charAt(code.length() - 1);
    }
}

