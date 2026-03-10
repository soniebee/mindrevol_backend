package com.mindrevol.core.modules.user.repository;

import com.mindrevol.core.modules.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, String> {

    /**
     * Lấy danh sách tài khoản xã hội của user
     */
    List<SocialAccount> findByUserId(String userId);

    /**
     * Kiểm tra xem user có tài khoản xã hội nào chưa
     */
    boolean existsByUserIdAndProvider(String userId, String provider);

    /**
     * Kiểm tra social account đã tồn tại chưa (dùng cho login)
     */
    boolean existsByProviderAndProviderId(String provider, String providerId);

    /**
     * Xóa tài khoản xã hội theo provider
     */
    void deleteByUserIdAndProvider(String userId, String provider);

    /**
     * Tìm tài khoản xã hội theo provider và providerId
     */
    java.util.Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);
}

