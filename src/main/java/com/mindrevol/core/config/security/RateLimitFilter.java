package com.mindrevol.core.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.service.RateLimitingService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String uri = request.getRequestURI();

        Bucket bucket;

        if (isLoginOrRegistrationEndpoint(uri)) {
            bucket = rateLimitingService.resolveLoginBucket(ip);
        } else if (isOtpOrTwoFactorVerificationEndpoint(uri)) {
            bucket = rateLimitingService.resolveOtpBucket(ip);
        } else if (uri.contains("/journeys/join")) {
            bucket = rateLimitingService.resolveStrictBucket(ip);
        } else if (uri.startsWith("/api/v1/")) {
            bucket = rateLimitingService.resolveGeneralBucket(ip);
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

            log.warn("Rate limit exceeded for IP: {} on URI: {}", ip, uri);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitForRefill));

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    429,
                    "Too many requests. Please try again in " + waitForRefill + " seconds.",
                    "RATE_LIMIT_EXCEEDED"
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private boolean isLoginOrRegistrationEndpoint(String uri) {
        return uri.startsWith("/api/v1/auth/login")
                || uri.startsWith("/api/v1/auth/register");
    }

    private boolean isOtpOrTwoFactorVerificationEndpoint(String uri) {
        return uri.equals("/api/v1/auth/otp/send")
                || uri.equals("/api/v1/auth/otp/login")
                || uri.equals("/api/v1/auth/2fa/methods/verify-login");
    }

    private String getClientIP(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}