package com.mindrevol.core.modules.auth.service.impl;
import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.utils.JwtUtil;
import com.mindrevol.core.modules.auth.dto.RedisUserSession;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionsGroupedResponse;
import com.mindrevol.core.modules.auth.service.SessionService;
import com.mindrevol.core.modules.auth.service.UserAgentParserService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final UserAgentParserService userAgentParserService;
    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;
    @Value("${app.session.location.enabled:true}")
    private boolean sessionLocationEnabled;
    @Value("${app.session.location.lookup-url:https://ipapi.co/%s/json/}")
    private String sessionLocationLookupUrl;
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String FALLBACK_DEVICE_PREFIX = "fp_";
    private record LocationInfo(String city, String country, String location) {
    }
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank() && !"unknown".equalsIgnoreCase(cfConnectingIp)) {
            return cfConnectingIp.trim();
        }
        return request.getRemoteAddr();
    }
    private String normalizeUserAgent(String userAgent) {
        return userAgent == null ? "" : userAgent.trim();
    }
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
    private String sanitizeDeviceId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }
    private String buildFallbackDeviceId(String userAgent, String clientIp) {
        String material = (userAgent == null ? "" : userAgent) + "|" + (clientIp == null ? "" : clientIp);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return FALLBACK_DEVICE_PREFIX + toHex(hash, 16);
        } catch (NoSuchAlgorithmException ex) {
            return FALLBACK_DEVICE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
    private String toHex(byte[] bytes, int maxChars) {
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
            if (builder.length() >= maxChars) {
                break;
            }
        }
        return builder.length() > maxChars ? builder.substring(0, maxChars) : builder.toString();
    }
    private String resolveDeviceId(HttpServletRequest request, String bodyDeviceId, String normalizedUserAgent, String clientIp) {
        String headerDeviceId = request != null ? request.getHeader("X-Device-Id") : null;
        String candidate = firstNonBlank(headerDeviceId, bodyDeviceId);
        if (candidate != null) {
            return sanitizeDeviceId(candidate);
        }
        return buildFallbackDeviceId(normalizedUserAgent, clientIp);
    }
    private Long resolveLoginAt(RedisUserSession session, String refreshToken) {
        if (session.getLoginAt() != null && session.getLoginAt() > 0) {
            return session.getLoginAt();
        }
        if (session.getCreatedAt() != null && session.getCreatedAt() > 0) {
            return session.getCreatedAt();
        }
        return jwtUtil.getIssuedAtMillis(refreshToken);
    }
    private RedisUserSession getSessionSafely(String sessionKey, String userSessionsKey, String refreshToken) {
        try {
            Object rawSession = redisTemplate.opsForValue().get(sessionKey);
            if (rawSession == null) {
                return null;
            }
            if (rawSession instanceof RedisUserSession redisUserSession) {
                return redisUserSession;
            }
            log.warn("Unexpected redis session type at key {}: {}", sessionKey, rawSession.getClass().getName());
            redisTemplate.delete(sessionKey);
            if (userSessionsKey != null && refreshToken != null) {
                redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
            }
            return null;
        } catch (SerializationException | ClassCastException ex) {
            log.warn("Invalid redis session payload at key {}. Cleaning stale entry.", sessionKey, ex);
            redisTemplate.delete(sessionKey);
            if (userSessionsKey != null && refreshToken != null) {
                redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
            }
            return null;
        }
    }
    private boolean hasLocation(RedisUserSession session) {
        return session.getLocation() != null && !session.getLocation().isBlank();
    }
    private boolean hasDeviceMetadata(RedisUserSession session) {
        return session.getDeviceId() != null && !session.getDeviceId().isBlank()
                && session.getOs() != null && !session.getOs().isBlank()
                && session.getBrowser() != null && !session.getBrowser().isBlank()
                && session.getDeviceName() != null && !session.getDeviceName().isBlank();
    }
    private void persistSessionKeepingTtl(String sessionKey, RedisUserSession session) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }
        Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.MILLISECONDS);
        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(sessionKey, session, ttl, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(sessionKey, session);
        }
    }
    private boolean isPublicIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isMulticastAddress()) {
                return false;
            }
            byte[] bytes = address.getAddress();
            if (bytes.length == 4) {
                int first = bytes[0] & 0xFF;
                int second = bytes[1] & 0xFF;
                if (first == 100 && second >= 64 && second <= 127) {
                    return false;
                }
            }
            if (bytes.length == 16) {
                int first = bytes[0] & 0xFF;
                if ((first & 0xFE) == 0xFC) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }
    private LocationInfo resolveLocationFromIp(String ipAddress) {
        if (!sessionLocationEnabled) {
            return null;
        }
        if (!isPublicIpAddress(ipAddress)) {
            return new LocationInfo(null, null, "Local/Private network");
        }
        try {
            String lookupUrl = String.format(sessionLocationLookupUrl, ipAddress);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = restTemplate.getForObject(lookupUrl, Map.class);
            if (payload == null) {
                return null;
            }
            String city = payload.get("city") instanceof String value ? value : null;
            String country = payload.get("country_name") instanceof String value ? value : null;
            if ((country == null || country.isBlank()) && payload.get("country") instanceof String value) {
                country = value;
            }
            String location = null;
            if (city != null && !city.isBlank() && country != null && !country.isBlank()) {
                location = city + ", " + country;
            } else if (country != null && !country.isBlank()) {
                location = country;
            } else if (city != null && !city.isBlank()) {
                location = city;
            }
            if (location == null || location.isBlank()) {
                return null;
            }
            return new LocationInfo(city, country, location);
        } catch (RestClientException ex) {
            log.debug("IP location lookup failed for {}", ipAddress, ex);
            return null;
        }
    }
    private RedisUserSession enrichLocationIfNeeded(RedisUserSession session, String sessionKey) {
        if (session == null || hasLocation(session)) {
            return session;
        }
        LocationInfo locationInfo = resolveLocationFromIp(session.getIpAddress());
        if (locationInfo == null) {
            return session;
        }
        session.setCity(locationInfo.city());
        session.setCountry(locationInfo.country());
        session.setLocation(locationInfo.location());
        persistSessionKeepingTtl(sessionKey, session);
        return session;
    }
    private RedisUserSession enrichDeviceMetadataIfNeeded(RedisUserSession session, String sessionKey) {
        if (session == null || hasDeviceMetadata(session)) {
            return session;
        }
        String userAgent = normalizeUserAgent(session.getUserAgent());
        UserAgentParserService.UserAgentInfo parsed = userAgentParserService.parse(userAgent);
        if (session.getDeviceId() == null || session.getDeviceId().isBlank()) {
            session.setDeviceId(buildFallbackDeviceId(userAgent, session.getIpAddress()));
        }
        if (session.getOs() == null || session.getOs().isBlank()) {
            session.setOs(parsed.os());
        }
        if (session.getBrowser() == null || session.getBrowser().isBlank()) {
            session.setBrowser(parsed.browser());
        }
        if (session.getDeviceName() == null || session.getDeviceName().isBlank()) {
            session.setDeviceName(parsed.deviceName());
        }
        persistSessionKeepingTtl(sessionKey, session);
        return session;
    }
    @Override
    public JwtResponse createTokenAndSession(User user, HttpServletRequest request, String deviceId) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtUtil.generateAccessToken(user, sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(user, sessionId);
        long now = System.currentTimeMillis();
        String userAgent = normalizeUserAgent(request != null ? request.getHeader("User-Agent") : null);
        String clientIp = resolveClientIp(request);
        String resolvedDeviceId = resolveDeviceId(request, deviceId, userAgent, clientIp);
        UserAgentParserService.UserAgentInfo parsedInfo = userAgentParserService.parse(userAgent);
        RedisUserSession redisSession = RedisUserSession.builder()
                .id(sessionId)
                .email(user.getEmail())
                .refreshToken(refreshToken)
                .ipAddress(clientIp)
                .userAgent(userAgent)
                .deviceId(resolvedDeviceId)
                .os(parsedInfo.os())
                .browser(parsedInfo.browser())
                .deviceName(parsedInfo.deviceName())
                .city(null)
                .country(null)
                .location(null)
                .loginAt(now)
                .createdAt(now)
                .expiredAt(now + refreshTokenExpirationMs)
                .build();
        LocationInfo locationInfo = resolveLocationFromIp(clientIp);
        if (locationInfo != null) {
            redisSession.setCity(locationInfo.city());
            redisSession.setCountry(locationInfo.country());
            redisSession.setLocation(locationInfo.location());
        }
        String redisKey = SESSION_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(redisKey, redisSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForSet().add(userSessionsKey, refreshToken);
        redisTemplate.expire(userSessionsKey, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        log.info("Saved session for user: {}", user.getEmail());
        return JwtResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }
    @Override
    public JwtResponse refreshToken(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = getSessionSafely(redisKey, null, null);
        if (session == null) {
            throw new BadRequestException("Invalid Refresh Token");
        }
        User user = userRepository.findByEmail(session.getEmail()).orElseThrow();
        String sessionId = session.getId() != null && !session.getId().isBlank()
                ? session.getId()
                : UUID.randomUUID().toString();
        String newAccessToken = jwtUtil.generateAccessToken(user, sessionId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user, sessionId);
        long now = System.currentTimeMillis();
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
        redisTemplate.delete(redisKey);
        Long loginAt = resolveLoginAt(session, refreshToken);
        String userAgent = normalizeUserAgent(session.getUserAgent());
        String resolvedDeviceId = session.getDeviceId() != null && !session.getDeviceId().isBlank()
                ? session.getDeviceId()
                : buildFallbackDeviceId(userAgent, session.getIpAddress());
        UserAgentParserService.UserAgentInfo parsedInfo = userAgentParserService.parse(userAgent);
        RedisUserSession newSession = RedisUserSession.builder()
                .id(sessionId)
                .email(user.getEmail())
                .refreshToken(newRefreshToken)
                .ipAddress(session.getIpAddress())
                .userAgent(userAgent)
                .deviceId(resolvedDeviceId)
                .os(session.getOs() != null && !session.getOs().isBlank() ? session.getOs() : parsedInfo.os())
                .browser(session.getBrowser() != null && !session.getBrowser().isBlank() ? session.getBrowser() : parsedInfo.browser())
                .deviceName(session.getDeviceName() != null && !session.getDeviceName().isBlank() ? session.getDeviceName() : parsedInfo.deviceName())
                .city(session.getCity())
                .country(session.getCountry())
                .location(session.getLocation())
                .loginAt(loginAt != null ? loginAt : now)
                .createdAt(session.getCreatedAt() != null ? session.getCreatedAt() : now)
                .expiredAt(now + refreshTokenExpirationMs)
                .build();
        String newRedisKey = SESSION_PREFIX + newRefreshToken;
        redisTemplate.opsForValue().set(newRedisKey, newSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForSet().add(userSessionsKey, newRefreshToken);
        redisTemplate.expire(userSessionsKey, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        return JwtResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
    }
    @Override
    public void logout(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = getSessionSafely(redisKey, null, null);
        if (session != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getEmail();
            redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
            redisTemplate.delete(redisKey);
        }
    }
    @Override
    public UserSessionsGroupedResponse getAllSessions(String userEmail, String currentTokenRaw, HttpServletRequest request) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userEmail;
        Set<Object> refreshTokens = redisTemplate.opsForSet().members(userSessionsKey);
        List<UserSessionResponse> responses = new ArrayList<>();
        String currentToken = currentTokenRaw != null && currentTokenRaw.startsWith("Bearer ")
                ? currentTokenRaw.substring(7)
                : currentTokenRaw;
        String currentSessionId = currentToken != null ? jwtUtil.getSessionId(currentToken) : null;
        String requestIp = resolveClientIp(request);
        String requestUserAgent = normalizeUserAgent(request != null ? request.getHeader("User-Agent") : null);
        String requestDeviceId = resolveDeviceId(request, null, requestUserAgent, requestIp);
        if (refreshTokens != null) {
            for (Object tokenObj : refreshTokens) {
                String token = String.valueOf(tokenObj);
                String sessionKey = SESSION_PREFIX + token;
                RedisUserSession session = getSessionSafely(sessionKey, userSessionsKey, token);
                if (session == null) {
                    continue;
                }
                session = enrichLocationIfNeeded(session, sessionKey);
                session = enrichDeviceMetadataIfNeeded(session, sessionKey);
                Long loginAt = resolveLoginAt(session, token);
                boolean isCurrent = false;
                if (currentSessionId != null && !currentSessionId.isBlank()) {
                    isCurrent = currentSessionId.equals(session.getId());
                } else if (currentToken != null) {
                    isCurrent = token.equals(currentToken);
                }
                if (!isCurrent && currentSessionId == null && requestDeviceId != null) {
                    isCurrent = requestDeviceId.equals(session.getDeviceId());
                }
                if (!isCurrent && currentSessionId == null && request != null) {
                    isCurrent = session.getIpAddress() != null
                            && session.getIpAddress().equals(requestIp)
                            && normalizeUserAgent(session.getUserAgent()).equals(requestUserAgent);
                }
                responses.add(UserSessionResponse.builder()
                        .id(session.getId())
                        .ipAddress(session.getIpAddress())
                        .userAgent(session.getUserAgent())
                        .os(session.getOs())
                        .browser(session.getBrowser())
                        .deviceName(session.getDeviceName())
                        .city(session.getCity())
                        .country(session.getCountry())
                        .location(session.getLocation())
                        .loginAt(loginAt != null ? UserSessionResponse.mapToLocalDateTime(loginAt) : null)
                        .expiresAt(UserSessionResponse.mapToLocalDateTime(session.getExpiredAt()))
                        .isCurrent(isCurrent)
                        .build());
            }
        }
        responses.sort((a, b) -> {
            int currentOrder = Boolean.compare(b.isCurrent(), a.isCurrent());
            if (currentOrder != 0) {
                return currentOrder;
            }
            if (a.getLoginAt() == null && b.getLoginAt() == null) {
                return 0;
            }
            if (a.getLoginAt() == null) {
                return 1;
            }
            if (b.getLoginAt() == null) {
                return -1;
            }
            return b.getLoginAt().compareTo(a.getLoginAt());
        });
        UserSessionResponse currentSession = null;
        List<UserSessionResponse> otherSessions = new ArrayList<>();
        for (UserSessionResponse sessionResponse : responses) {
            if (sessionResponse.isCurrent() && currentSession == null) {
                currentSession = sessionResponse;
                continue;
            }
            otherSessions.add(sessionResponse);
        }
        return UserSessionsGroupedResponse.builder()
                .currentSession(currentSession)
                .otherSessions(otherSessions)
                .build();
    }
    @Override
    public void revokeSession(String sessionId, String userEmail) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userEmail;
        Set<Object> refreshTokens = redisTemplate.opsForSet().members(userSessionsKey);
        if (refreshTokens != null) {
            for (Object tokenObj : refreshTokens) {
                String token = String.valueOf(tokenObj);
                String sessionKey = SESSION_PREFIX + token;
                RedisUserSession session = getSessionSafely(sessionKey, userSessionsKey, token);
                if (session != null && sessionId.equals(session.getId())) {
                    redisTemplate.delete(sessionKey);
                    redisTemplate.opsForSet().remove(userSessionsKey, token);
                    return;
                }
            }
        }
        throw new ResourceNotFoundException("Session not found");
    }
}
