package com.mindrevol.core.modules.box.repo;

import com.mindrevol.core.modules.box.entity.BoxMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // Nhớ import thư viện UUID này nha

@Repository
public interface BoxMemberRepository extends JpaRepository<BoxMember, UUID> { // Đã đổi String thành UUID ở đây

    // Kiểm tra user có trong box không
    boolean existsByBoxIdAndUserId(UUID boxId, UUID userId);

    // Lấy thông tin thành viên cụ thể
    Optional<BoxMember> findByBoxIdAndUserId(UUID boxId, UUID userId);

    // Đếm số lượng thành viên (để xử lý logic ẩn/hiện Box Chat)
    long countByBoxId(UUID boxId);

    // Phân trang danh sách thành viên trong một Box
    Page<BoxMember> findByBoxId(UUID boxId, Pageable pageable);
}