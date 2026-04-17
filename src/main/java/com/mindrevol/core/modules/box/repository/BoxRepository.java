package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.box.entity.Box;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoxRepository extends JpaRepository<Box, String> {
    // Lấy danh sách các Box mà user đang là thành viên
    @Query("SELECT b FROM Box b JOIN b.members m WHERE m.user.id = :userId")
    Page<Box> findMyBoxes(@Param("userId") String userId, Pageable pageable);
}