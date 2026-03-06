package com.mindrevol.core.modules.auth.strategy;

import com.mindrevol.core.modules.auth.dto.SocialUserInfo;

/**
 * Strategy Interface cho Social Login
 * Mỗi provider (Google, Apple, Facebook) sẽ implement interface này
 *
 * Lợi ích:
 * - Open/Closed Principle: mở rộng dễ (thêm Apple/Facebook)
 * - Không cần sửa core logic
 * - Test dễ (mock strategy)
 */
public interface SocialLoginStrategy {

    /**
     * Xác thực token từ social provider
     * @param token ID token hoặc access token từ provider
     * @return SocialUserInfo chứa email, name, picture, provider, providerId
     * @throws Exception nếu token không hợp lệ
     */
    SocialUserInfo authenticate(String token) throws Exception;

    /**
     * Trả về provider name (google, apple, facebook, etc.)
     */
    String getProviderName();
}

