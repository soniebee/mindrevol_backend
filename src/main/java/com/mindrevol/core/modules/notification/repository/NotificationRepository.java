package com.mindrevol.core.modules.notification.repository;

import com.mindrevol.core.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId")
    void markAllAsRead(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :userId")
    void deleteAllByRecipientId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
