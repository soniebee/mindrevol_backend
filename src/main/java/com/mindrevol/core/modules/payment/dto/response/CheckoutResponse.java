package com.mindrevol.core.modules.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutResponse {
    private String paymentUrl;
    private String transactionId;
}