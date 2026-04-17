package com.mindrevol.core.modules.payment.job;

import com.mindrevol.core.modules.payment.entity.PaymentStatus;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import com.mindrevol.core.modules.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupJob {

    private final PaymentTransactionRepository paymentTransactionRepository;

    // Chạy mỗi 15 phút một lần
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void cleanupStaleTransactions() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        List<PaymentTransaction> staleTransactions = paymentTransactionRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, thirtyMinutesAgo);

        if (!staleTransactions.isEmpty()) {
            staleTransactions.forEach(tx -> {
                tx.setStatus(PaymentStatus.CANCELLED);
                log.info("Đã hủy giao dịch quá hạn: {}", tx.getId());
            });
            paymentTransactionRepository.saveAll(staleTransactions);
        }
    }
}