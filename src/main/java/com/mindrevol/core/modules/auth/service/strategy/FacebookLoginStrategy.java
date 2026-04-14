package com.mindrevol.core.modules.auth.service.strategy;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.modules.auth.dto.request.FacebookLoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FacebookLoginStrategy implements SocialLoginStrategy {

    private final RestTemplate restTemplate;

    @Value("${facebook.app-id:}")
    private String facebookAppId;

    @Value("${facebook.app-secret:}")
    private String facebookAppSecret;

    @Override
    public String getProviderName() {
        return "facebook";
    }

    @Override
    public SocialProviderData verifyAndGetData(Object data) {
        try {
            String accessToken = resolveAccessToken(data);
            verifyTokenOwnership(accessToken);

            String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/me")
                    .queryParam("fields", "id,name,email,picture.width(200).height(200)")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            Map<String, Object> fbProfile = restTemplate.getForObject(url, Map.class);
            if (fbProfile == null || !fbProfile.containsKey("email")) {
                throw new BadRequestException("Unable to get email from Facebook.");
            }

            String avatarUrl = null;
            try {
                Map<String, Object> picture = (Map<String, Object>) fbProfile.get("picture");
                Map<String, Object> picData = (Map<String, Object>) picture.get("data");
                avatarUrl = (String) picData.get("url");
            } catch (Exception ignored) {}

            return SocialProviderData.builder()
                    .providerId((String) fbProfile.get("id"))
                    .email((String) fbProfile.get("email"))
                    .name((String) fbProfile.get("name"))
                    .avatarUrl(avatarUrl)
                    .build();
        } catch (Exception e) {
            if (e instanceof BadRequestException) {
                throw (BadRequestException) e;
            }
            throw new BadRequestException("Facebook authentication failed.");
        }
    }

    private String resolveAccessToken(Object data) {
        String accessToken;
        if (data instanceof FacebookLoginRequest) {
            accessToken = ((FacebookLoginRequest) data).getAccessToken();
        } else {
            accessToken = (String) data;
        }

        if (!StringUtils.hasText(accessToken)) {
            throw new BadRequestException("Facebook access token is required.");
        }
        return accessToken;
    }

    @SuppressWarnings("unchecked")
    private void verifyTokenOwnership(String accessToken) {
        if (!StringUtils.hasText(facebookAppId) || !StringUtils.hasText(facebookAppSecret)) {
            throw new BadRequestException("Facebook login is not configured.");
        }

        String appAccessToken = facebookAppId + "|" + facebookAppSecret;
        String debugUrl = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/debug_token")
                .queryParam("input_token", accessToken)
                .queryParam("access_token", appAccessToken)
                .toUriString();

        Map<String, Object> debugResponse = restTemplate.getForObject(debugUrl, Map.class);
        if (debugResponse == null || !(debugResponse.get("data") instanceof Map)) {
            throw new BadRequestException("Invalid Facebook token response.");
        }

        Map<String, Object> tokenData = (Map<String, Object>) debugResponse.get("data");
        Object isValid = tokenData.get("is_valid");
        Object appId = tokenData.get("app_id");

        if (!Boolean.TRUE.equals(isValid) || !facebookAppId.equals(String.valueOf(appId))) {
            throw new BadRequestException("Invalid Facebook access token.");
        }
    }
}