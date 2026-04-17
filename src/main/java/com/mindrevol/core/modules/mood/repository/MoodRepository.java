package com.mindrevol.core.modules.mood.repository;

import com.mindrevol.core.modules.mood.entity.Mood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodRepository extends JpaRepository<Mood, String> {

    // 1. Lấy tất cả trạng thái CÒN HẠN trong một Box (Phục vụ API get danh sách)
    List<Mood> findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(String boxId, LocalDateTime now);

    // 2. Tìm trạng thái hiện tại của 1 user trong Box (Phục vụ API đăng/xóa trạng thái)
    Optional<Mood> findByBoxIdAndUserIdAndExpiresAtAfter(String boxId, String userId, LocalDateTime now);

    // =========================================================================
    // 🔥 CÁC HÀM DƯỚI ĐÂY DÀNH CHO JOB CHẠY NGẦM (DỌN RÁC)
    // =========================================================================

    // 3. Dọn các lượt thả tim của các Mood đã quá hạn
    @Modifying
    @Query(value = "DELETE FROM mood_reactions WHERE mood_id IN (SELECT id FROM moods WHERE expires_at <= :now)", nativeQuery = true)
    void hardDeleteExpiredReactions(@Param("now") LocalDateTime now);

    // 4. Dọn các Mood đã quá hạn
    @Modifying
    @Query(value = "DELETE FROM moods WHERE expires_at <= :now", nativeQuery = true)
    void hardDeleteExpiredMoods(@Param("now") LocalDateTime now);
}