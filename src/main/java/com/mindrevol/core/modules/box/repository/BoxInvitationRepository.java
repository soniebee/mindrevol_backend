package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.box.entity.BoxInvitation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoxInvitationRepository extends JpaRepository<BoxInvitation, Long> {

    List<BoxInvitation> findByInviteeIdAndStatus(String inviteeId, String status);

    boolean existsByBoxIdAndInviteeIdAndStatus(String boxId, String inviteeId, String status);

    @EntityGraph(attributePaths = {"box", "inviter"})
    List<BoxInvitation> findAllByInviteeIdAndStatusOrderByCreatedAtDesc(String inviteeId, String status);
    
    // Thêm hàm lấy danh sách Lời mời có TÌM KIẾM THEO TÊN BOX
    @EntityGraph(attributePaths = {"box", "inviter"})
    @Query("SELECT i FROM BoxInvitation i WHERE i.invitee.id = :inviteeId AND i.status = :status " +
           "AND (:search IS NULL OR :search = '' OR LOWER(i.box.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<BoxInvitation> findAllByInviteeIdAndStatusAndSearchOrderByCreatedAtDesc(
            @Param("inviteeId") String inviteeId, 
            @Param("status") String status, 
            @Param("search") String search);

    Optional<BoxInvitation> findByBoxIdAndInviteeIdAndStatus(String boxId, String inviteeId, String string);
}