package com.mindrevol.core.modules.notification.repository;

import com.mindrevol.core.modules.notification.entity.Notification;
import com.mindrevol.core.modules.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId ORDER BY n.updatedAt DESC, n.createdAt DESC")
    Page<Notification> findByRecipientIdOrderByUpdatedAtDescCreatedAtDesc(@Param("userId") String userId, Pageable pageable);

    // [TASK-102] Đếm số thông báo CHƯA XEM (để hiển thị badge đỏ ở icon chuông)
    long countByRecipientIdAndIsSeenFalse(String userId);


    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.isSeen = true WHERE n.recipient.id = :userId")
    void markAllAsRead(@Param("userId") String userId);

    // [TASK-102] Đánh dấu tất cả đã xem (khi mở Panel thông báo)
    @Modifying
    @Query("UPDATE Notification n SET n.isSeen = true WHERE n.recipient.id = :userId AND n.isSeen = false")
    void markAllAsSeen(@Param("userId") String userId);

    // [TASK-101] Tìm thông báo chưa đọc cùng loại, cùng tham chiếu để gộp
    Optional<Notification> findFirstByRecipientIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
            String recipientId, NotificationType type, String referenceId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :userId")
    void deleteAllByRecipientId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = false AND n.createdAt < :cutoffDate")
    int deleteOldUnreadNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}