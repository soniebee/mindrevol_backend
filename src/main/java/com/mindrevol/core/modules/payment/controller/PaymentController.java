package com.mindrevol.core.modules.payment.controller;

import com.mindrevol.core.common.dto.ApiResponse;
import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.payment.dto.request.CheckoutRequest;
import com.mindrevol.core.modules.payment.dto.response.CheckoutResponse;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import com.mindrevol.core.modules.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // API: Tạo yêu cầu thanh toán (Yêu cầu phải đăng nhập / có JWT)
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createCheckout(
            @RequestBody CheckoutRequest request,
            HttpServletRequest httpServletRequest) {
        
        String userId = SecurityUtils.getCurrentUserId();
        
        // Lấy IP client (Cần thiết cho VNPay)
        String clientIp = httpServletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpServletRequest.getRemoteAddr();
        }

        CheckoutResponse response = paymentService.createCheckout(userId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response, "Khởi tạo thanh toán thành công"));
    }

    // =========================================================
    // API MỚI BỔ SUNG: Dùng để Frontend gọi check status (Polling)
    // =========================================================
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentTransaction>> getTransactionStatus(@PathVariable String transactionId) {
        // Hàm này gọi xuống Service bạn đã viết sẵn từ trước
        PaymentTransaction transaction = paymentService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Lấy thông tin giao dịch thành công"));
    }

    // API: Webhook nhận kết quả từ đối tác (PUBLIC - Đã cấu hình permitAll trong SecurityConfig)
    @PostMapping("/webhook/{provider}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String provider,
            HttpServletRequest request,
            @RequestBody(required = false) String payload) {
        
        try {
            PaymentProvider paymentProvider = PaymentProvider.valueOf(provider.toUpperCase());
            paymentService.processWebhook(paymentProvider, request, payload);
            
            // Trả về HTTP 200 OK để báo với VNPay/MoMo/Stripe là đã nhận thành công
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid provider");
        } catch (Exception e) {
            // Log chi tiết và trả về 400 để đối tác gửi lại (retry) nếu lỗi server
            return ResponseEntity.badRequest().body("Error processing webhook");
        }
    }
}