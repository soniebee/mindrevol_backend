package com.mindrevol.core.modules.payment.service.strategy;

import com.mindrevol.core.modules.payment.dto.response.PaymentCallbackData;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentStrategy {
    PaymentProvider getProvider();
    
    // Tạo URL thanh toán trả về cho Frontend
    String createPaymentUrl(PaymentTransaction transaction, String clientIp);
    
    // Xác thực Webhook (chống giả mạo chữ ký)
    boolean verifyWebhookSignature(HttpServletRequest request, String payload);
    
    // Trích xuất chi tiết dữ liệu (Mã, Số tiền, Trạng thái) từ Webhook để Backend kiểm tra chéo
    PaymentCallbackData extractCallbackData(HttpServletRequest request, String payload);
}