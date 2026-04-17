package com.mindrevol.core.modules.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCallbackData {
    private String transactionId;        // Mã giao dịch nội bộ của hệ thống (lưu trong DB)
    private String providerTransactionId; // Mã giao dịch riêng của đối tác (để đối soát)
    private Long amount;                 // Số tiền thực tế người dùng đã trả
    private boolean isSuccess;           // Trạng thái thanh toán (thành công hay thất bại)
}