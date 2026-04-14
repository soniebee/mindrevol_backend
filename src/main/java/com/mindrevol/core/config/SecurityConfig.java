package com.mindrevol.core.config;

import com.mindrevol.core.config.security.CustomAuthenticationEntryPoint;
import com.mindrevol.core.config.security.JwtAuthenticationFilter;
import com.mindrevol.core.config.security.RateLimitFilter;
import com.mindrevol.core.config.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter authenticationJwtTokenFilter;
    private final CustomAuthenticationEntryPoint unauthorizedHandler;
    @Autowired(required = false)
    private RateLimitFilter rateLimitFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .xssProtection(HeadersConfigurer.XXssConfig::disable)
                .contentSecurityPolicy(csp -> csp.policyDirectives("script-src 'self'"))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            )
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/v1/auth/register/**",
                        "/api/v1/auth/check-email",
                        "/api/v1/auth/check-handle",
                        "/api/v1/auth/complete",
                        "/api/v1/auth/login",
                        "/api/v1/auth/login/**",
                        "/api/v1/auth/refresh-token",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/magic-link",
                        "/api/v1/auth/magic-login"
                ).permitAll()
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/otp/send",
                        "/api/v1/auth/otp/login",
                        "/api/v1/auth/2fa/methods/verify-login"
                ).permitAll()
                .requestMatchers(
                        "/uploads/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/ws/**",
                        "/api/v1/plans/{shareableLink}/public",
                        "/actuator/health",
                        "/api/v1/payment/webhook",
                        "/actuator/prometheus"
                ).permitAll()
                .requestMatchers(
                        "/api/v1/auth/logout",
                        "/api/v1/auth/change-password",
                        "/api/v1/auth/sessions",
                        "/api/v1/auth/sessions/**"
                ).authenticated()
                .requestMatchers("/api/v1/**").authenticated()
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/files/upload").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable);

        http.authenticationProvider(authenticationProvider());
        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterBefore(authenticationJwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            configuration.setAllowedOrigins(Arrays.asList(
                "https://mindrevol.vercel.app",
                "https://mindrevol-web.vercel.app",
                "http://localhost:5173"
            ));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept", "Apikey", "X-Device-Id", "x-device-id"));
        configuration.setExposedHeaders(List.of("X-Rate-Limit-Remaining", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}