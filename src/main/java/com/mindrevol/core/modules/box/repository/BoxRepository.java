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
    
    // Lấy TẤT CẢ các Box (Có tìm kiếm)
    @Query("SELECT b FROM Box b JOIN b.members m WHERE m.user.id = :userId " +
           "AND (:search IS NULL OR :search = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Box> findMyBoxes(@Param("userId") String userId, @Param("search") String search, Pageable pageable);

    // Lấy Box CÁ NHÂN (Do mình làm Owner, có tìm kiếm)
    @Query("SELECT b FROM Box b JOIN b.members m WHERE m.user.id = :userId AND b.owner.id = :userId " +
           "AND (:search IS NULL OR :search = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Box> findMyPersonalBoxes(@Param("userId") String userId, @Param("search") String search, Pageable pageable);

    // Lấy Box BẠN BÈ (Do người khác làm Owner, có tìm kiếm)
    @Query("SELECT b FROM Box b JOIN b.members m WHERE m.user.id = :userId AND b.owner.id != :userId " +
           "AND (:search IS NULL OR :search = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Box> findMyFriendBoxes(@Param("userId") String userId, @Param("search") String search, Pageable pageable);
}