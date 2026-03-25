package com.mindrevol.core.modules.auth.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.modules.auth.dto.BackupCodeStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing backup codes with status tracking.
 * 
 * Storage format in database:
 * [
 *   {"code": "hashed_code", "used": false, "usedAt": null},
 *   {"code": "hashed_code_2", "used": true, "usedAt": "2026-03-21T10:15:00"}
 * ]
 */
@Slf4j
@Component
public class BackupCodeUtil {

    private final ObjectMapper objectMapper;

    public BackupCodeUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse backup codes from JSON string stored in database
     */
    public List<BackupCodeStatusDto> parseBackupCodes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<BackupCodeStatusDto>>() {});
        } catch (Exception ex) {
            try {
                List<String> legacyCodes = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                return createBackupCodes(legacyCodes);
            } catch (Exception legacyEx) {
                log.warn("Cannot parse backup codes JSON, returning empty list", legacyEx);
                return new ArrayList<>();
            }
        }
    }

    /**
     * Convert backup codes list to JSON string for storage
     */
    public String serializeBackupCodes(List<BackupCodeStatusDto> codes) {
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (Exception ex) {
            log.error("Failed to serialize backup codes", ex);
            throw new IllegalStateException("Failed to persist backup codes", ex);
        }
    }

    /**
     * Create backup code status objects from plain codes (for initial generation)
     */
    public List<BackupCodeStatusDto> createBackupCodes(List<String> plainCodes) {
        List<BackupCodeStatusDto> result = new ArrayList<>();
        for (String code : plainCodes) {
            result.add(BackupCodeStatusDto.builder()
                    .code(code)
                    .used(false)
                    .usedAt(null)
                    .build());
        }
        return result;
    }

    /**
     * Get count of unused codes
     */
    public int countUnusedCodes(List<BackupCodeStatusDto> codes) {
        return (int) codes.stream()
                .filter(code -> !code.isUsed())
                .count();
    }

    /**
     * Mark a backup code as used and save
     * Returns true if code was found and marked as used, false otherwise
     */
    public boolean consumeBackupCode(List<BackupCodeStatusDto> codes, String incomingHash) {
        for (BackupCodeStatusDto code : codes) {
            if (!code.isUsed() && code.getCode().equals(incomingHash)) {
                code.setUsed(true);
                code.setUsedAt(LocalDateTime.now());
                return true;
            }
        }
        return false;
    }

    /**
     * Create display format for backup codes file (for download)
     */
    public String formatBackupCodesForDownload(List<String> plainCodes, LocalDateTime generatedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("MindRevol 2FA Backup Codes\n");
        sb.append("Generated: ").append(generatedAt).append("\n");
        sb.append("Keep these codes in a safe place. Each code can be used only once.\n");
        sb.append("---\n\n");
        
        for (String code : plainCodes) {
            sb.append(code).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Get list of plain codes (used/unused) - for admin purposes or status check
     */
    public List<String> extractCodes(List<BackupCodeStatusDto> codes) {
        return codes.stream()
                .map(BackupCodeStatusDto::getCode)
                .toList();
    }

    /**
     * Filter codes by used status
     */
    public List<BackupCodeStatusDto> filterByUsedStatus(List<BackupCodeStatusDto> codes, boolean used) {
        return codes.stream()
                .filter(code -> code.isUsed() == used)
                .toList();
    }
}


