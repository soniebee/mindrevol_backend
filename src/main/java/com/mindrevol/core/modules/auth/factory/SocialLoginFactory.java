package com.mindrevol.core.modules.auth.factory;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.modules.auth.strategy.GoogleLoginStrategy;
import com.mindrevol.core.modules.auth.strategy.SocialLoginStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory Pattern cho Social Login Strategies
 *
 * Lợi ích:
 * - Quản lý tất cả strategies ở một chỗ
 * - Dễ thêm strategy mới (Apple, Facebook, TikTok)
 * - Client code không cần biết chi tiết strategy
 *
 * Usage:
 * SocialLoginStrategy strategy = factory.getStrategy("google");
 * SocialUserInfo userInfo = strategy.authenticate(token);
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLoginFactory {

    private final GoogleLoginStrategy googleStrategy;
    // Sau này thêm: AppleLoginStrategy, FacebookLoginStrategy, etc.

    private final Map<String, SocialLoginStrategy> strategies = new HashMap<>();

    // Constructor initialization
    public SocialLoginFactory(GoogleLoginStrategy googleStrategy) {
        this.googleStrategy = googleStrategy;
        registerStrategies();
    }

    /**
     * Đăng ký tất cả strategies có sẵn
     */
    private void registerStrategies() {
        strategies.put("google", googleStrategy);
        // Sau này:
        // strategies.put("apple", appleStrategy);
        // strategies.put("facebook", facebookStrategy);
        log.info("Registered social login strategies: {}", strategies.keySet());
    }

    /**
     * Lấy strategy theo provider name
     */
    public SocialLoginStrategy getStrategy(String provider) {
        if (provider == null || provider.isEmpty()) {
            throw new BadRequestException("Provider name is required");
        }

        SocialLoginStrategy strategy = strategies.get(provider.toLowerCase());
        if (strategy == null) {
            throw new BadRequestException("Social login provider '" + provider + "' is not supported");
        }

        return strategy;
    }

    /**
     * Kiểm tra provider có được hỗ trợ không
     */
    public boolean isProviderSupported(String provider) {
        return strategies.containsKey(provider.toLowerCase());
    }

    /**
     * Lấy danh sách tất cả provider hỗ trợ
     */
    public java.util.Set<String> getSupportedProviders() {
        return strategies.keySet();
    }
}

