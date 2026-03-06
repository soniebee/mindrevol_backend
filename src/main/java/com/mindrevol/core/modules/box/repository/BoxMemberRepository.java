package com.mindrevol.core.modules.box.repository;

import com.mindrevol.core.modules.box.entity.BoxMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoxMemberRepository extends JpaRepository<BoxMember, String> {

    boolean existsByBoxIdAndUserId(String boxId, String userId);

    Optional<BoxMember> findByBoxIdAndUserId(String boxId, String userId);

    long countByBoxId(String boxId);

    Page<BoxMember> findByBoxId(String boxId, Pageable pageable);
}