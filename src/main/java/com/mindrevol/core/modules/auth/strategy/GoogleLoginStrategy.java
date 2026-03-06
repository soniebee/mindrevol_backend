package com.mindrevol.core.modules.auth.strategy;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.modules.auth.dto.SocialUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import java.util.Collections;

/**
 * Google OAuth2 Login Strategy
 * Xác thực idToken từ Google Identity SDK
 */
@Slf4j
@Component
public class GoogleLoginStrategy implements SocialLoginStrategy {

    @Value("${app.oauth2.google.client-id}")
    private String googleClientId;

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String PROVIDER_NAME = "google";

    /**
     * Verify Google ID Token
     *
     * Flow:
     * 1. Frontend nhận idToken từ Google Identity SDK
     * 2. Frontend gửi idToken tới Backend
     * 3. Backend verify idToken bằng Google API
     * 4. Extract email, name, picture từ idToken
     */
    @Override
    public SocialUserInfo authenticate(String idToken) throws Exception {
        log.info("Authenticating Google token...");

        try {
            // 1. Tạo verifier để xác thực token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Verify token
            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                log.error("Invalid Google ID token");
                throw new BadRequestException("Invalid Google ID token");
            }

            // 3. Extract payload
            GoogleIdToken.Payload payload = token.getPayload();

            // 4. Xây dựng SocialUserInfo từ payload
            SocialUserInfo userInfo = SocialUserInfo.builder()
                    .email((String) payload.get("email"))
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .provider(PROVIDER_NAME)
                    .providerId(payload.getSubject())  // Google Sub ID
                    .build();

            log.info("Google authentication successful for email: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
            throw new BadRequestException("Google authentication failed: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}

