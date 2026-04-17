package com.mindrevol.core.modules.mood.repository;

import com.mindrevol.core.modules.mood.entity.MoodReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MoodReactionRepository extends JpaRepository<MoodReaction, String> {

    Optional<MoodReaction> findByMoodIdAndUserId(String moodId, String userId);

    // 🔥 PRO MAX: Dùng @Modifying và @Query để xóa 1 phát bay luôn (Bulk Delete).
    @Modifying
    @Query("DELETE FROM MoodReaction mr WHERE mr.mood.id = :moodId")
    void deleteAllByMoodId(@Param("moodId") String moodId);
}