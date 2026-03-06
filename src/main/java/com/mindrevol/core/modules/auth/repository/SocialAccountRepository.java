package com.mindrevol.core.modules.auth.repository;

import com.mindrevol.core.modules.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho SocialAccount
 */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, String> {

    /**
     * Tìm social account theo provider và provider ID
     * VD: Google + "12345678" (Google Sub)
     */
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Kiểm tra social account đã tồn tại chưa
     */
    boolean existsByProviderAndProviderId(String provider, String providerId);
}

