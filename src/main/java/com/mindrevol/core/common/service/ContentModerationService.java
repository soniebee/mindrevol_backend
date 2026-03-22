package com.mindrevol.core.common.service;
import com.mindrevol.core.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
@Service
@Slf4j
public class ContentModerationService {
    @Value("${app.moderation.api-user:}")
    private String apiUser;
    @Value("${app.moderation.api-secret:}")
    private String apiSecret;
    @Value("${app.moderation.enabled:false}")
    private boolean isEnabled;
    private final RestTemplate restTemplate = new RestTemplate();
    public void validateImage(String imageUrl) {
        if (!isEnabled || apiUser.isEmpty()) {
            log.debug("Content moderation is disabled or missing keys.");
            return;
        }
        try {
            String apiUrl = String.format(
                "https://api.sightengine.com/1.0/check.json?models=nudity,wad,offensive&api_user=%s&api_secret=%s&url=%s",
                apiUser, apiSecret, imageUrl
            );
            ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && "success".equals(body.get("status"))) {
                Map<String, Object> nudity = (Map<String, Object>) body.get("nudity");
                if (nudity != null) {
                    Double safe = (Double) nudity.get("safe");
                    if (safe != null && safe < 0.15) {
                        throw new BadRequestException("Image contains sensitive content (nudity).");
                    }
                }
                Map<String, Object> offensive = (Map<String, Object>) body.get("offensive");
                if (offensive != null) {
                    Double prob = (Double) offensive.get("prob");
                    if (prob != null && prob > 0.8) {
                        throw new BadRequestException("Image contains offensive or sensitive content.");
                    }
                }
                log.info("Image passed moderation check: {}", imageUrl);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Moderation API error", e);
        }
    }
}