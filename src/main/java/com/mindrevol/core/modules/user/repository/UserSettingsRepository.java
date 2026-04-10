package com.mindrevol.core.modules.user.repository;

import com.mindrevol.core.modules.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, String> {
    Optional<UserSettings> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM UserSettings s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);
}