package com.mindrevol.core.modules.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mindrevol.core.modules.user.entity.User;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Service xử lý JWT Token
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret:your-secret-key-must-be-at-least-256-bits-long-for-hs512-algorithm}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:3600000}") // 1 hour
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:86400000}") // 24 hours
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Tạo Access Token
     */
    public String generateAccessToken(User user) {
        String roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("roles", roles)
                .claim("userId", user.getId())
                .claim("handle", user.getHandle())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Tạo Refresh Token
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Lấy email từ token
     */
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lấy thời gian hết hạn của Access Token (tính bằng giây)
     */
    public Long getAccessTokenExpirationInSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}

