package com.mindrevol.core.modules.payment.service.impl;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException; // Phải có dòng này
import com.mindrevol.core.modules.notification.service.NotificationDispatchService;
import com.mindrevol.core.modules.payment.dto.request.CheckoutRequest;
import com.mindrevol.core.modules.payment.dto.response.CheckoutResponse;
import com.mindrevol.core.modules.payment.dto.response.PaymentCallbackData;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import com.mindrevol.core.modules.payment.entity.PaymentStatus;
import com.mindrevol.core.modules.payment.entity.PaymentTransaction;
import com.mindrevol.core.modules.payment.repository.PaymentTransactionRepository;
import com.mindrevol.core.modules.payment.service.PaymentService;
import com.mindrevol.core.modules.payment.service.strategy.PaymentStrategy;
import com.mindrevol.core.modules.payment.service.strategy.PaymentStrategyFactory;
import com.mindrevol.core.modules.user.entity.AccountType;
import com.mindrevol.core.modules.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentStrategyFactory strategyFactory;
    private final UserService userService;
    private final NotificationDispatchService notificationDispatchService;

    @Override
    @Transactional
    public CheckoutResponse createCheckout(String userId, CheckoutRequest request, String clientIp) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(userId)
                .provider(request.getProvider())
                .packageType(request.getPackageType())
                .amount(request.getPackageType().getPriceVnd())
                .status(PaymentStatus.PENDING)
                .build();
        transactionRepository.save(transaction);

        PaymentStrategy strategy = strategyFactory.getStrategy(request.getProvider());
        String paymentUrl = strategy.createPaymentUrl(transaction, clientIp);

        return CheckoutResponse.builder()
                .paymentUrl(paymentUrl)
                .transactionId(transaction.getId())
                .build();
    }

    @Override
    @Transactional
    public void processWebhook(PaymentProvider provider, HttpServletRequest request, String payload) {
        PaymentStrategy strategy = strategyFactory.getStrategy(provider);

        if (!strategy.verifyWebhookSignature(request, payload)) {
            log.error("Cảnh báo: Sai chữ ký Webhook từ nhà cung cấp {}", provider);
            throw new BadRequestException("Invalid signature");
        }

        PaymentCallbackData callbackData = strategy.extractCallbackData(request, payload);

        PaymentTransaction transaction = transactionRepository.findByIdWithLock(callbackData.getTransactionId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giao dịch: " + callbackData.getTransactionId()));

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Giao dịch {} đã được xử lý trước đó, bỏ qua.", callbackData.getTransactionId());
            return; 
        }

        if (!transaction.getAmount().equals(callbackData.getAmount())) {
            log.error("Cảnh báo Hack/Lỗi: Giao dịch {} số tiền không khớp. Cần: {}, Nhận: {}", 
                    transaction.getId(), transaction.getAmount(), callbackData.getAmount());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setProviderTransactionId(callbackData.getProviderTransactionId());
            transactionRepository.save(transaction);
            return;
        }

        if (!callbackData.isSuccess()) {
            transaction.setStatus(PaymentStatus.FAILED);
            transactionRepository.save(transaction);
            return;
        }

        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setProviderTransactionId(callbackData.getProviderTransactionId());
        transactionRepository.save(transaction);

        userService.upgradeUserTier(
                transaction.getUserId(), 
                AccountType.GOLD, 
                transaction.getPackageType().getDurationDays()
        );

        notificationDispatchService.dispatchPaymentSuccessWebSocket(
                transaction.getUserId(), 
                transaction.getPackageType().name()
        );
        
        log.info("Xử lý thành công giao dịch {} cho User {}", transaction.getId(), transaction.getUserId());
    }
    
    @Override
    public PaymentTransaction getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionId));
    }
}