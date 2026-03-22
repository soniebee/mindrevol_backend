package com.mindrevol.core.modules.auth.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.modules.auth.dto.request.AppleLoginRequest;
import com.mindrevol.core.modules.auth.util.AppleAuthUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleLoginStrategy implements SocialLoginStrategy {

    private final AppleAuthUtil appleAuthUtil;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return "apple";
    }

    @Override
    public SocialProviderData verifyAndGetData(Object data) {
        if (!(data instanceof AppleLoginRequest)) {
            throw new BadRequestException("Invalid data for Apple Login");
        }
        AppleLoginRequest request = (AppleLoginRequest) data;

        try {
            Claims claims = appleAuthUtil.validateToken(request.getIdentityToken());
            String email = claims.get("email", String.class);
            String sub = claims.getSubject();

            if (email == null) throw new BadRequestException("Apple did not return an email.");

            String name = "Apple User";
            if (request.getUser() != null) {
                try {
                    JsonNode node = objectMapper.readTree(request.getUser());
                    JsonNode nameNode = node.get("name");
                    if (nameNode != null) {
                        String fName = nameNode.has("firstName") ? nameNode.get("firstName").asText() : "";
                        String lName = nameNode.has("lastName") ? nameNode.get("lastName").asText() : "";
                        name = (fName + " " + lName).trim();
                    }
                } catch (Exception ignored) {}
            }

            return SocialProviderData.builder()
                    .providerId(sub)
                    .email(email)
                    .name(name)
                    .build();

        } catch (Exception e) {
            throw new BadRequestException("Apple Auth Failed: " + e.getMessage());
        }
    }
}