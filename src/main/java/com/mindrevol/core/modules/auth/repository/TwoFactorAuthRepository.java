package com.mindrevol.core.modules.auth.repository;

import com.mindrevol.core.modules.auth.entity.TwoFactorAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, String> {
    
    /**
     * Find all 2FA methods for a specific user
     */
    List<TwoFactorAuth> findByUserId(String userId);
    
    /**
     * Find a specific 2FA method for a user
     */
    Optional<TwoFactorAuth> findByUserIdAndMethod(String userId, String method);
    
    /**
     * Find all enabled 2FA methods for a user
     */
    List<TwoFactorAuth> findByUserIdAndEnabledTrue(String userId);
    
    /**
     * Check if user has any enabled 2FA method
     */
    boolean existsByUserIdAndEnabledTrue(String userId);
    
    /**
     * Delete all 2FA methods for a user
     */
    void deleteByUserId(String userId);
    
    /**
     * Delete a specific 2FA method for a user
     */
    void deleteByUserIdAndMethod(String userId, String method);
}
