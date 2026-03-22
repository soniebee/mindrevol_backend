package com.mindrevol.core.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TwoFactorBackupCodesResponse {

    @ArraySchema(schema = @Schema(example = "ABCD3EFG7H"), arraySchema = @Schema(description = "One-time recovery codes. Show once and store securely."))
    private List<String> backupCodes;
}


