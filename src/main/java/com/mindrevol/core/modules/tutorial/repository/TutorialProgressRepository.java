package com.mindrevol.core.modules.tutorial.repository;

import com.mindrevol.core.modules.tutorial.entity.TutorialProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorialProgressRepository extends JpaRepository<TutorialProgress, Long> { // Lưu ý: kiểu ID của TutorialProgress (Long) giữ nguyên, vì nó kế thừa từ BaseEntity

    // Đổi tham số từ Long sang String
    Optional<TutorialProgress> findByUserId(String userId);
}