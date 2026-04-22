package com.mindrevol.core.modules.recap.service.strategy;
import java.util.List;

public interface RecapGeneratorStrategy {
    // Thêm tham số delayMs
    String generateVideo(List<String> imageUrls, String audioUrl, int delayMs) throws Exception;
}