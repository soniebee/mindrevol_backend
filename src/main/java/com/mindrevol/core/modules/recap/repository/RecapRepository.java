package com.mindrevol.core.modules.recap.repository;

import com.mindrevol.core.modules.recap.entity.Recap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecapRepository extends JpaRepository<Recap, String> {
    List<Recap> findAllByUserIdOrderByCreatedAtDesc(String userId);
    
    // SỬA DÒNG NÀY: Thêm chữ First và OrderByCreatedAtDesc
    Optional<Recap> findFirstByJourneyIdOrderByCreatedAtDesc(String journeyId); 
}