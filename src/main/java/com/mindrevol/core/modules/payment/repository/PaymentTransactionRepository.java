package com.mindrevol.core.modules.payment.repository;

import com.mindrevol.core.modules.payment.entity.PaymentStatus;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    // [QUAN TRỌNG] Khóa row (FOR UPDATE) khi tìm kiếm để xử lý Webhook.
    // Đảm bảo không có 2 thread nào xử lý cùng 1 giao dịch tại cùng 1 thời điểm.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentTransaction p WHERE p.id = :id")
    Optional<PaymentTransaction> findByIdWithLock(@Param("id") String id);

    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);

    // Dùng cho Job dọn dẹp giao dịch rác
    List<PaymentTransaction> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);
}