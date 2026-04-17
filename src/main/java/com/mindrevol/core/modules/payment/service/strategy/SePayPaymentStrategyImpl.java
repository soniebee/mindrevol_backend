package com.mindrevol.core.modules.payment.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.core.modules.payment.dto.response.PaymentCallbackData;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SePayPaymentStrategyImpl implements PaymentStrategy {

    @Value("${sepay.webhook-token}")
    private String sepayWebhookToken;

    @Value("${sepay.bank-name}") 
    private String bankId;

    @Value("${sepay.bank-account}")
    private String bankAccount;
    
    @Value("${sepay.account-name}")
    private String accountName;

    @Value("${sepay.prefix}")
    private String transferPrefix;

    private final ObjectMapper objectMapper;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.SEPAY;
    }

    @Override
    public String createPaymentUrl(PaymentTransaction transaction, String clientIp) {
        try {
            String addInfo = transferPrefix + transaction.getId();
            String accountNameEncoded = URLEncoder.encode(accountName, StandardCharsets.UTF_8.toString());
            String addInfoEncoded = URLEncoder.encode(addInfo, StandardCharsets.UTF_8.toString());
            
            String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankId, 
                bankAccount, 
                String.valueOf(transaction.getAmount()), 
                addInfoEncoded, 
                accountNameEncoded
            );

            return qrUrl;
        } catch (Exception e) {
            log.error("Lỗi tạo VietQR link", e);
            throw new RuntimeException("Cannot create VietQR link");
        }
    }

    @Override
    public boolean verifyWebhookSignature(HttpServletRequest request, String payload) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Apikey ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return token.equals(sepayWebhookToken);
    }

    @Override
    public PaymentCallbackData extractCallbackData(HttpServletRequest request, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            
            String providerTransactionId = root.path("id").asText();
            long amount = root.path("transferAmount").asLong();
            
            // ĐÃ SỬA: Không dùng toUpperCase() ở đây nữa để bảo toàn format gốc
            String content = root.path("content").asText(); 
            String transferType = root.path("transferType").asText();

            String transactionId = extractTransactionIdFromContent(content);
            if (transactionId == null) {
                log.warn("Không tìm thấy Transaction ID hợp lệ trong CK: {}", content);
                transactionId = "UNKNOWN"; 
            }

            return PaymentCallbackData.builder()
                    .transactionId(transactionId)
                    .providerTransactionId(providerTransactionId)
                    .amount(amount)
                    .isSuccess("in".equalsIgnoreCase(transferType))
                    .build();

        } catch (Exception e) {
            log.error("Lỗi parse SePay payload", e);
            throw new RuntimeException("Invalid SePay payload");
        }
    }

    private String extractTransactionIdFromContent(String content) {
        String regex = "(?i)" + transferPrefix + "([a-zA-Z0-9-]{3,})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String extractedId = matcher.group(1).toLowerCase();
            
            // FIX: Ngân hàng tự động xóa mất dấu gạch ngang (-) trong nội dung chuyển khoản.
            // UUID gốc có 36 ký tự (chuẩn 8-4-4-4-12). Nếu bị xóa gạch ngang sẽ còn đúng 32 ký tự.
            // Ta cần khôi phục lại dấu gạch ngang để Database tìm thấy.
            if (extractedId.length() == 32 && !extractedId.contains("-")) {
                extractedId = extractedId.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
                );
            }
            
            return extractedId; 
        }
        return null;
    }
}