package com.mindrevol.core.modules.box.repo;

import com.mindrevol.core.modules.box.entity.BoxInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoxInvitationRepository extends JpaRepository<BoxInvitation, UUID> {

    // Lấy danh sách lời mời đang chờ của một User
    List<BoxInvitation> findByRecipientIdAndStatus(UUID recipientId, String status);

    // Kiểm tra xem đã gửi lời mời cho người này chưa để tránh spam
    boolean existsByBoxIdAndRecipientIdAndStatus(UUID boxId, UUID recipientId, String status);
}