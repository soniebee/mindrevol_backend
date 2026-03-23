package com.mindrevol.core.modules.mood.repository;

import com.mindrevol.core.modules.mood.entity.MoodReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MoodReactionRepository extends JpaRepository<MoodReaction, String> {
    Optional<MoodReaction> findByMoodIdAndUserId(String moodId, String userId);
}