package com.mindrevol.core.modules.box.repo;

import com.mindrevol.core.modules.box.entity.Box;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository

public interface BoxRepository extends JpaRepository<Box, UUID> {

    // Tìm các Box mà User là thành viên và sắp xếp theo hoạt động mới nhất
    @Query("SELECT b FROM Box b JOIN b.members m WHERE m.user.id = :userId ORDER BY b.lastActivityAt DESC")
    Page<Box> findMyBoxes(@Param("userId") String userId, Pageable pageable);
}