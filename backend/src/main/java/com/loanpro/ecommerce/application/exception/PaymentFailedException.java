package com.loanpro.ecommerce.application.exception;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String reason) {
        super(reason);
    }
}
