package com.mindrevol.core.modules.user.repository;

import com.mindrevol.core.modules.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, String> {

    /**
     * Kiểm tra xem user có bị chặn bởi user khác không
     */
    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    /**
     * Xóa record chặn giữa 2 user
     */
    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);
}

