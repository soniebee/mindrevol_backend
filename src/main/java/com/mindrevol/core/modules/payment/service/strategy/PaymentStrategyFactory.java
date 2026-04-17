package com.mindrevol.core.modules.payment.service.strategy;

import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {
    private final Map<PaymentProvider, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PaymentStrategy::getProvider, strategy -> strategy));
    }

    public PaymentStrategy getStrategy(PaymentProvider provider) {
        if (!strategies.containsKey(provider)) {
            throw new IllegalArgumentException("Chưa hỗ trợ cổng thanh toán: " + provider);
        }
        return strategies.get(provider);
    }
}