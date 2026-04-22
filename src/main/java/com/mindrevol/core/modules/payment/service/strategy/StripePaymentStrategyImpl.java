package com.mindrevol.core.modules.payment.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.modules.payment.dto.response.PaymentCallbackData;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.net.Webhook;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentStrategyImpl implements PaymentStrategy {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.return-url:http://localhost:5173/payment/status}")
    private String returnUrl;

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public String createPaymentUrl(PaymentTransaction transaction, String clientIp) {
        try {
            // Quy đổi tiền: Nếu đang là VND (Ví dụ 99,000 VND), Stripe hỗ trợ VND trực tiếp
            // Hoặc bạn có thể convert sang USD nếu muốn. Ở đây ví dụ dùng VND.
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(returnUrl + "?status=success")
                    .setCancelUrl(returnUrl + "?status=cancel")
                    .setClientReferenceId(transaction.getId()) // Lưu ID nội bộ vào đây
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("vnd")
                                                    .setUnitAmount(transaction.getAmount())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Nâng cấp gói " + transaction.getPackageType().name())
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl(); // Trả về URL Checkout của Stripe
        } catch (Exception e) {
            log.error("Lỗi tạo Stripe Checkout Session", e);
            throw new RuntimeException("Cannot create Stripe session");
        }
    }

    @Override
    public boolean verifyWebhookSignature(HttpServletRequest request, String payload) {
        String sigHeader = request.getHeader("Stripe-Signature");
        try {
            // Hàm này của SDK Stripe tự động băm payload và so sánh với sigHeader
            Webhook.Signature.verifyHeader(payload, sigHeader, webhookSecret, 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PaymentCallbackData extractCallbackData(HttpServletRequest request, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText();
            
            // Chỉ quan tâm sự kiện thanh toán thành công
            boolean isSuccess = "checkout.session.completed".equals(type);
            
            JsonNode dataObject = root.path("data").path("object");
            String transactionId = dataObject.path("client_reference_id").asText();
            String stripeSessionId = dataObject.path("id").asText();
            long amountTotal = dataObject.path("amount_total").asLong(); // Stripe lưu giá trị thực

            return PaymentCallbackData.builder()
                    .transactionId(transactionId)
                    .providerTransactionId(stripeSessionId)
                    .amount(amountTotal)
                    .isSuccess(isSuccess)
                    .build();
        } catch (Exception e) {
            log.error("Lỗi parse payload Stripe webhook", e);
            throw new RuntimeException("Invalid payload");
        }
    }
}