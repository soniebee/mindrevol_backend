package com.mindrevol.core.modules.payment.dto.request;

import com.mindrevol.core.modules.payment.entity.PackageType;
import com.mindrevol.core.modules.payment.entity.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotNull(message = "Provider is required")
    private PaymentProvider provider;
    
    @NotNull(message = "Package type is required")
    private PackageType packageType;
}