package com.mindrevol.core.modules.payment.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.modules.payment.dto.response.PaymentCallbackData;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoMoPaymentStrategyImpl implements PaymentStrategy {

    @Value("${momo.partner-code:}")
    private String partnerCode;

    @Value("${momo.access-key:}")
    private String accessKey;

    @Value("${momo.secret-key:}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Value("${momo.return-url:http://localhost:5173/payment/status}")
    private String returnUrl;

    @Value("${momo.notify-url:https://YOUR_NGROK_DOMAIN/api/v1/payment/webhook/momo}")
    private String notifyUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MOMO;
    }

    @Override
    public String createPaymentUrl(PaymentTransaction transaction, String clientIp) {
        try {
            String orderId = transaction.getId();
            String requestId = orderId + "-" + System.currentTimeMillis();
            String orderInfo = "Nâng cấp gói " + transaction.getPackageType().name();
            String amount = String.valueOf(transaction.getAmount());
            String extraData = "";
            String requestType = "captureWallet";

            // Tạo chuỗi signature theo chuẩn MoMo
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + notifyUrl +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + returnUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = hmacSHA256(secretKey, rawSignature);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("partnerName", "MindRevol");
            requestBody.put("storeId", "MindRevolStore");
            requestBody.put("requestId", requestId);
            requestBody.put("amount", Long.parseLong(amount));
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", returnUrl);
            requestBody.put("ipnUrl", notifyUrl);
            requestBody.put("lang", "vi");
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", requestType);
            requestBody.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Gọi API MoMo
            String responseStr = restTemplate.postForObject(endpoint, entity, String.class);
            JsonNode responseJson = objectMapper.readTree(responseStr);

            if (responseJson.has("payUrl")) {
                return responseJson.get("payUrl").asText();
            } else {
                log.error("MoMo API Error: {}", responseStr);
                throw new RuntimeException("Không lấy được URL thanh toán từ MoMo");
            }

        } catch (Exception e) {
            log.error("Lỗi tạo URL MoMo", e);
            throw new RuntimeException("Cannot create MoMo payment", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(HttpServletRequest request, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String providedSignature = root.path("signature").asText();

            // MoMo yêu cầu ghép chuỗi theo đúng thứ tự Alphabet để hash lại
            String rawHash = "accessKey=" + accessKey +
                    "&amount=" + root.path("amount").asText() +
                    "&extraData=" + root.path("extraData").asText() +
                    "&message=" + root.path("message").asText() +
                    "&orderId=" + root.path("orderId").asText() +
                    "&orderInfo=" + root.path("orderInfo").asText() +
                    "&orderType=" + root.path("orderType").asText() +
                    "&partnerCode=" + root.path("partnerCode").asText() +
                    "&payType=" + root.path("payType").asText() +
                    "&requestId=" + root.path("requestId").asText() +
                    "&responseTime=" + root.path("responseTime").asText() +
                    "&resultCode=" + root.path("resultCode").asText() +
                    "&transId=" + root.path("transId").asText();

            String generatedSignature = hmacSHA256(secretKey, rawHash);
            return generatedSignature.equals(providedSignature);
        } catch (Exception e) {
            log.error("Lỗi xác thực chữ ký MoMo", e);
            return false;
        }
    }

    @Override
    public PaymentCallbackData extractCallbackData(HttpServletRequest request, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String transactionId = root.path("orderId").asText();
            String providerTransactionId = root.path("transId").asText();
            long amount = root.path("amount").asLong();
            int resultCode = root.path("resultCode").asInt();

            return PaymentCallbackData.builder()
                    .transactionId(transactionId)
                    .providerTransactionId(providerTransactionId)
                    .amount(amount)
                    .isSuccess(resultCode == 0) // resultCode = 0 là thành công
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Invalid MoMo payload");
        }
    }

    private String hmacSHA256(String key, String data) {
        try {
            Mac hmac256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac256.init(secretKey);
            return Hex.encodeHexString(hmac256.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC SHA256", e);
        }
    }
}