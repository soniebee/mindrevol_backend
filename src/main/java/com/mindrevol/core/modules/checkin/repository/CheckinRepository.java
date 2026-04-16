package com.mindrevol.core.modules.checkin.repository;

import com.mindrevol.core.modules.checkin.dto.response.CalendarRecapResponse;
import com.mindrevol.core.modules.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, String> {

    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey IS NOT NULL " +
           "AND (c.user.id = :userId " +
           "OR c.user.id IN (SELECT f.addressee.id FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'ACCEPTED') " +
           "OR c.user.id IN (SELECT f.requester.id FROM Friendship f WHERE f.addressee.id = :userId AND f.status = 'ACCEPTED')) " +
           "ORDER BY c.createdAt DESC")
    Page<Checkin> findJourneyGridFeed(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Checkin c WHERE c.user.id = :userId AND c.journey IS NULL ORDER BY c.createdAt DESC")
    Page<Checkin> findArchivedCheckinsByUser(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") String journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(String journeyId, String userId);
    
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(String journeyId);

    List<Checkin> findByJourneyIdAndUserId(String journeyId, String id);
    
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT c FROM Checkin c " +
    	       "LEFT JOIN JourneyParticipant jp ON c.journey.id = jp.journey.id AND c.user.id = jp.user.id " +
    	       "WHERE c.journey.id = :journeyId " +
    	       // Nếu là Viewer thì thấy hết. Nếu là người lạ, thì xem người đó có bật public không (nếu là guest), 
    	       // hoặc mặc định cho phép xem nếu họ là Box Member (jp.id IS NULL)
    	       "AND (:isViewerMember = true OR jp.isProfileVisible = true OR jp.id IS NULL) " +
    	       "ORDER BY c.createdAt DESC")
    	List<Checkin> findVisibleCheckinsByJourneyId(@Param("journeyId") String journeyId, @Param("isViewerMember") boolean isViewerMember);

    @Query("SELECT c FROM Checkin c JOIN FETCH c.user u WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) AND c.createdAt >= :sinceDate AND c.createdAt <= :cursor AND u.id NOT IN :excludedUserIds ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeedRecent(@Param("userId") String userId, @Param("sinceDate") LocalDateTime sinceDate, @Param("cursor") LocalDateTime cursor, @Param("excludedUserIds") Collection<String> excludedUserIds, Pageable pageable);

    @Query("SELECT c FROM Checkin c JOIN FETCH c.user u WHERE c.journey.id = :journeyId AND c.createdAt <= :cursor AND u.id NOT IN :excludedUserIds ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") String journeyId, @Param("cursor") LocalDateTime cursor, @Param("excludedUserIds") Collection<String> excludedUserIds, Pageable pageable);
    
    @Query("SELECT c.createdAt FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId AND c.status IN ('NORMAL', 'LATE', 'COMEBACK', 'REST') ORDER BY c.createdAt DESC")
    List<LocalDateTime> findValidCheckinDates(@Param("journeyId") String journeyId, @Param("userId") String userId);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Checkin> findMyCheckinsInJourney(@Param("journeyId") String journeyId, @Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.status <> 'REJECTED'")
    List<Checkin> findMediaByJourneyId(@Param("journeyId") String journeyId);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByJourney(@Param("journeyId") String journeyId);

    @Query("SELECT c FROM Checkin c WHERE c.journey.box.id = :boxId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByBox(@Param("boxId") String boxId);
    
    @Query("SELECT c FROM Checkin c WHERE c.user.id = :userId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByUser(@Param("userId") String userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT c.imageUrl FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.imageUrl != '' ORDER BY c.createdAt DESC")
    java.util.List<String> findPreviewImagesByJourneyId(@org.springframework.data.repository.query.Param("journeyId") String journeyId, org.springframework.data.domain.Pageable pageable);
    
    @Query(value = "SELECT " +
            "  CAST(EXTRACT(DAY FROM created_at) AS INTEGER) AS day, " +
            "  image_url AS imageUrl, " +
            "  id AS checkinId " +
            "FROM (" +
            "  SELECT id, created_at, image_url, " +
            "         ROW_NUMBER() OVER(PARTITION BY EXTRACT(DAY FROM created_at) ORDER BY created_at DESC) as rn " +
            "  FROM checkins " +
            "  WHERE user_id = :userId " +
            "    AND EXTRACT(MONTH FROM created_at) = :month " +
            "    AND EXTRACT(YEAR FROM created_at) = :year " +
            "    AND deleted_at IS NULL " +
            "    AND image_url IS NOT NULL " +
            ") AS subquery " +
            "WHERE rn = 1", 
    nativeQuery = true)
    List<CalendarRecapResponse> getCalendarRecapInMonth(@Param("userId") String userId, 
                                                 @Param("year") int year, 
                                                 @Param("month") int month);
    
    long countByUserIdAndDeletedAtIsNull(String userId);
    
    long countByUserId(String userId);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.status <> 'REJECTED' ORDER BY c.checkinDate ASC")
    List<Checkin> findMediaByJourneyIdForRecap(@Param("journeyId") String journeyId);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId AND c.imageUrl IS NOT NULL AND c.status <> 'REJECTED' ORDER BY c.checkinDate ASC")
    List<Checkin> findMediaByJourneyIdAndUserId(@Param("journeyId") String journeyId, @Param("userId") String userId);

    // =========================================================================
    // [THÊM MỚI] CÁC HÀM LẤY ẢNH TỪ NHIỀU HÀNH TRÌNH CÙNG LÚC (GLOBAL RECAP)
    // =========================================================================

    @Query("SELECT c FROM Checkin c WHERE c.journey.id IN :journeyIds AND c.imageUrl IS NOT NULL AND c.imageUrl != '' AND c.status <> 'REJECTED' ORDER BY c.checkinDate ASC")
    List<Checkin> findMediaByMultipleJourneyIds(@Param("journeyIds") List<String> journeyIds);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id IN :journeyIds AND c.user.id = :userId AND c.imageUrl IS NOT NULL AND c.imageUrl != '' AND c.status <> 'REJECTED' ORDER BY c.checkinDate ASC")
    List<Checkin> findMediaByMultipleJourneyIdsAndUserId(@Param("journeyIds") List<String> journeyIds, @Param("userId") String userId);
}