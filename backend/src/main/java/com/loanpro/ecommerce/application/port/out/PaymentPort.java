package com.loanpro.ecommerce.application.port.out;

import java.math.BigDecimal;

public interface PaymentPort {
    PaymentResult processPayment(BigDecimal amount);
    String getPaymentMethod();
}
