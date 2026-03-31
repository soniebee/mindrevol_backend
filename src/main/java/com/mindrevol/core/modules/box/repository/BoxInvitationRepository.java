package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.journey.entity.JourneyInvitationStatus;
import com.mindrevol.core.modules.box.entity.BoxInvitation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoxInvitationRepository extends JpaRepository<BoxInvitation, Long> {

    // 🔥 Đã đổi RecipientId thành InviteeId
    List<BoxInvitation> findByInviteeIdAndStatus(String inviteeId, String status);

    // 🔥 Đã đổi RecipientId thành InviteeId (Dùng cho hàm inviteMember ở Service)
    boolean existsByBoxIdAndInviteeIdAndStatus(String boxId, String inviteeId, String status);

    @EntityGraph(attributePaths = {"box", "inviter"})
    List<BoxInvitation> findAllByInviteeIdAndStatusOrderByCreatedAtDesc(String inviteeId, String status);
    
 // Lấy một lời mời đang chờ xử lý
    Optional<BoxInvitation> findByBoxIdAndInviteeIdAndStatus(String boxId, String inviteeId, String string);
}