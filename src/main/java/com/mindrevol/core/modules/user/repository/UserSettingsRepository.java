package com.mindrevol.core.modules.user.repository;

import com.mindrevol.core.modules.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, String> {
    Optional<UserSettings> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM UserSettings s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);

    // [TỐI ƯU HÓA & FIX LỖI NULL] 
    // Chỉ lấy đúng User ID và Location Visibility để tối ưu tốc độ map và tránh lỗi primitive
    @Query("SELECT s.user.id, s.locationVisibility FROM UserSettings s WHERE s.user.id IN :userIds")
    List<Object[]> findLocationVisibilityByUserIdIn(@Param("userIds") Set<String> userIds);
}