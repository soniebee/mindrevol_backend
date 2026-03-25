package com.mindrevol.core.modules.auth.service.impl;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.common.utils.JwtUtil;
import com.mindrevol.core.modules.auth.dto.RedisUserSession;
import com.mindrevol.core.modules.auth.dto.response.JwtResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.core.modules.auth.dto.response.UserSessionsGroupedResponse;
import com.mindrevol.core.modules.auth.service.SessionService;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.session.location.enabled:true}")
    private boolean sessionLocationEnabled;

    @Value("${app.session.location.lookup-url:https://ipapi.co/%s/json/}")
    private String sessionLocationLookupUrl;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";

    private record LocationInfo(String city, String country, String location) {}

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

            // Unexpected type in Redis, clean it to avoid repeated runtime failures.
            log.warn("Unexpected redis session type at key {}: {}", sessionKey, rawSession.getClass().getName());
            redisTemplate.delete(sessionKey);
            if (userSessionsKey != null && refreshToken != null) {
                redisTemplate.opsForSet().remove(userSessionsKey, refreshToken);
            }
            return null;
        } catch (SerializationException | ClassCastException ex) {
            // Old serialized class metadata (renamed package/class) can land here.
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
                // Carrier-grade NAT range 100.64.0.0/10 is not globally routable.
                if (first == 100 && second >= 64 && second <= 127) {
                    return false;
                }
            }

            if (bytes.length == 16) {
                // IPv6 Unique Local Address fc00::/7 is not globally routable.
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

        if (sessionKey == null || sessionKey.isBlank()) {
            return session;
        }

        Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.MILLISECONDS);
        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(sessionKey, session, ttl, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(sessionKey, session);
        }
        return session;
    }

    @Override
    public JwtResponse createTokenAndSession(User user, HttpServletRequest request) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtUtil.generateAccessToken(user, sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(user, sessionId);
        long now = System.currentTimeMillis();
        String userAgent = normalizeUserAgent(request.getHeader("User-Agent"));
        String clientIp = resolveClientIp(request);

        RedisUserSession redisSession = RedisUserSession.builder()
                .id(sessionId)
                .email(user.getEmail())
                .refreshToken(refreshToken)
                .ipAddress(clientIp)
                .userAgent(userAgent)
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

        if (session == null) throw new BadRequestException("Invalid Refresh Token");
        
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
        
        RedisUserSession newSession = RedisUserSession.builder()
                .id(sessionId)
                .email(user.getEmail())
                .refreshToken(newRefreshToken)
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
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

        if (refreshTokens != null) {
            for (Object tokenObj : refreshTokens) {
                String token = String.valueOf(tokenObj);
                String sessionKey = SESSION_PREFIX + token;
                RedisUserSession session = getSessionSafely(sessionKey, userSessionsKey, token);

                if (session != null) {
                    session = enrichLocationIfNeeded(session, sessionKey);
                    Long loginAt = resolveLoginAt(session, token);
                    boolean isCurrent = false;
                    if (currentSessionId != null && !currentSessionId.isBlank()) {
                        isCurrent = currentSessionId.equals(session.getId());
                    } else if (currentToken != null) {
                        // Backward compatibility if caller still passes refresh token.
                        isCurrent = token.equals(currentToken);
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
                            .city(session.getCity())
                            .country(session.getCountry())
                            .location(session.getLocation())
                            .loginAt(loginAt != null ? UserSessionResponse.mapToLocalDateTime(loginAt) : null)
                            .expiresAt(UserSessionResponse.mapToLocalDateTime(session.getExpiredAt()))
                            .isCurrent(isCurrent)
                            .build());
                }
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

                if (session != null && session.getId().equals(sessionId)) {
                    redisTemplate.delete(sessionKey);
                    redisTemplate.opsForSet().remove(userSessionsKey, token);
                    return;
                }
            }
        }
        throw new ResourceNotFoundException("Session not found");
    }
}