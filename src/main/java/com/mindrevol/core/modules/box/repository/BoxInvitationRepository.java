package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.box.entity.BoxInvitation;
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

    // Nếu trong file của bạn có hàm nào chứa chữ Sender thì đổi thành Inviter luôn nhé!
}