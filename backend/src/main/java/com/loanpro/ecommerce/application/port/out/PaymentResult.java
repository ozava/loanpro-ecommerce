package com.loanpro.ecommerce.application.port.out;

public record PaymentResult(
        boolean success,
        String transactionId,
        String failureReason
) {
}
