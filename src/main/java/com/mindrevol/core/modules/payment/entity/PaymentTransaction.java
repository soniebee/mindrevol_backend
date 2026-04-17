package com.mindrevol.core.modules.payment.entity;

import com.mindrevol.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_transactions")
@SQLDelete(sql = "UPDATE payment_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class PaymentTransaction extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    // Mã giao dịch đối tác trả về (VD: mã của VNPay hoặc Charge ID của Stripe)
    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 50)
    private PackageType packageType;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Chuỗi kiểm tra tính hợp lệ, dùng để đối chiếu IPN/Webhook
    @Column(length = 255)
    private String signature;
}