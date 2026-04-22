package com.mindrevol.core.modules.payment.service;

import com.mindrevol.core.modules.payment.dto.request.CheckoutRequest;
import com.mindrevol.core.modules.payment.dto.response.CheckoutResponse;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    
    CheckoutResponse createCheckout(String userId, CheckoutRequest request, String clientIp);
    
    void processWebhook(PaymentProvider provider, HttpServletRequest request, String payload);
    
    // THÊM DÒNG NÀY ĐỂ FIX LỖI
    PaymentTransaction getTransactionById(String transactionId);
}