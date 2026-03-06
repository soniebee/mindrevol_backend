package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.box.entity.BoxInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoxInvitationRepository extends JpaRepository<BoxInvitation, String> {

    List<BoxInvitation> findByRecipientIdAndStatus(String recipientId, String status);

    boolean existsByBoxIdAndRecipientIdAndStatus(String boxId, String recipientId, String status);
}