package com.mindrevol.core.modules.payment.entity;

import lombok.Getter;

@Getter
public enum PackageType {
    // Để 2.000đ để pass mức tối thiểu của SePay (chuyển khoản liên ngân hàng)
    // Hoặc để 10.000đ nếu bạn test VNPay
    GOLD_1_MONTH(30, 2000),    // Chỗ này đổi thành 2000 (thay vì 99000)
    GOLD_6_MONTHS(180, 5000),  // Chỗ này đổi thành 5000 (thay vì 499000)
    GOLD_1_YEAR(365, 10000);   // Chỗ này đổi thành 10000 (thay vì 899000)

    private final int durationDays;
    private final long priceVnd;

    PackageType(int durationDays, long priceVnd) {
        this.durationDays = durationDays;
        this.priceVnd = priceVnd;
    }
}