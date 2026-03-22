package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.box.entity.BoxMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoxMemberRepository extends JpaRepository<BoxMember, String> { // Đã đổi String thành UUID ở đây

    // Kiểm tra user có trong box không
    boolean existsByBoxIdAndUserId(String boxId, String userId);

    // Lấy thông tin thành viên cụ thể
    Optional<BoxMember> findByBoxIdAndUserId(String boxId, String userId);

    // Đếm số lượng thành viên (để xử lý logic ẩn/hiện Box Chat)
    long countByBoxId(String boxId);

    // Phân trang danh sách thành viên trong một Box
    Page<BoxMember> findByBoxId(String boxId, Pageable pageable);
}