package com.mindrevol.core.modules.payment.service.strategy;

import com.mindrevol.core.modules.payment.dto.response.PaymentCallbackData;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayPaymentStrategyImpl implements PaymentStrategy {

	@Value("${vnpay.tmn-code}")
	private String vnpTmnCode;

	@Value("${vnpay.hash-secret}")
	private String vnpHashSecret;

    // Sửa lại tên biến map với file properties của bạn
    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url:http://localhost:5173/payment/status}")
    private String vnpReturnUrl;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.VNPAY;
    }

    @Override
    public String createPaymentUrl(PaymentTransaction transaction, String clientIp) {
    	
    	System.out.println("=====================================");
    	System.out.println(">>> ĐANG DÙNG VNPAY TMN_CODE: [" + vnpTmnCode + "]");
    	System.out.println("=====================================");
        
        // --- [FIX LỖI IP] Chuẩn hóa IPv6 thành IPv4 cho VNPay ---
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }
        // Nếu qua Proxy/Ngrok có nhiều IP (ngăn cách bằng dấu phẩy), chỉ lấy IP đầu tiên
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "127.0.0.1"; // Default an toàn
        }

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(transaction.getAmount() * 100)); // VNPay nhân 100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", transaction.getId());
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + transaction.getId());
        
        // --- [FIX LỖI THIẾU PARAMS] Bắt buộc phải có OrderType ---
        vnp_Params.put("vnp_OrderType", "other"); // "other" hoặc "billpayment" đều được
        
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", clientIp);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15); // Timeout 15p
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Build URL
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            return vnpPayUrl + "?" + queryUrl;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL VNPay", e);
        }
    }

    // ... (Các hàm verifyWebhookSignature và extractCallbackData giữ nguyên như cũ) ...

    @Override
    public boolean verifyWebhookSignature(HttpServletRequest request, String payload) {
        try {
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
                String fieldName = URLEncoder.encode(params.nextElement(), StandardCharsets.US_ASCII.toString());
                String fieldValue = URLEncoder.encode(request.getParameter(fieldName), StandardCharsets.US_ASCII.toString());
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    fields.put(fieldName, fieldValue);
                }
            }
            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = fields.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append("=").append(fieldValue);
                    if (itr.hasNext()) {
                        hashData.append("&");
                    }
                }
            }
            String signValue = hmacSHA512(vnpHashSecret, hashData.toString());
            return signValue.equals(vnp_SecureHash) && "00".equals(request.getParameter("vnp_ResponseCode"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PaymentCallbackData extractCallbackData(HttpServletRequest request, String payload) {
        String txnRef = request.getParameter("vnp_TxnRef");
        String providerTxnId = request.getParameter("vnp_TransactionNo");
        String responseCode = request.getParameter("vnp_ResponseCode");
        
        String amountStr = request.getParameter("vnp_Amount");
        long amount = (amountStr != null && !amountStr.isEmpty()) ? Long.parseLong(amountStr) / 100 : 0;
        
        boolean isSuccess = "00".equals(responseCode);

        return PaymentCallbackData.builder()
                .transactionId(txnRef)
                .providerTransactionId(providerTxnId)
                .amount(amount)
                .isSuccess(isSuccess)
                .build();
    }

    private String hmacSHA512(final String key, final String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(result);
        } catch (Exception e) {
            return "";
        }
    }
}