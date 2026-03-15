package com.mindrevol.core.modules.mood.repository;

import com.mindrevol.core.modules.mood.entity.Mood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodRepository extends JpaRepository<Mood, String> {
    // 1. Lấy tất cả trạng thái CÒN HẠN trong một Box
    List<Mood> findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(String boxId, LocalDateTime now);

    // 2. Tìm trạng thái hiện tại của 1 user trong Box (để ghi đè)
    Optional<Mood> findByBoxIdAndUserId(String boxId, String userId);
}
