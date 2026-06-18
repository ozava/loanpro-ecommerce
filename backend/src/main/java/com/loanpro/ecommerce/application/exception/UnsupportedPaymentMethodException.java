package com.loanpro.ecommerce.application.exception;

import java.util.Set;

public class UnsupportedPaymentMethodException extends RuntimeException {
    public UnsupportedPaymentMethodException(String paymentMethod, Set<String> supported) {
        super("Payment method [" + paymentMethod + "] is not supported. Supported methods: " + supported);
    }
}
