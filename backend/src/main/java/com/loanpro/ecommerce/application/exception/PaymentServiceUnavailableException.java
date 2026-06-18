package com.loanpro.ecommerce.application.exception;

public class PaymentServiceUnavailableException extends RuntimeException {
    public PaymentServiceUnavailableException(String paymentMethod) {
        super("Payment service [" + paymentMethod + "] is temporarily unavailable. Please try again later.");
    }
}
