package com.loanpro.ecommerce.infrastructure.adapter.payment;

import com.loanpro.ecommerce.application.exception.PaymentServiceUnavailableException;
import com.loanpro.ecommerce.application.port.out.PaymentPort;
import com.loanpro.ecommerce.application.port.out.PaymentResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Service
public class FakePaypalAdapter implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(FakePaypalAdapter.class);
    private final SecureRandom random = new SecureRandom();

    @Override
    public String getPaymentMethod() {
        return "paypal";
    }

    @Override
    @CircuitBreaker(name = "paypal", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        boolean failure = random.nextInt(100) < 10;

        if (failure) {
            log.warn("[PayPal] Payment declined. TransactionId: {}, Amount: {}, Reason: Simulated provider rejection",
                    transactionId, amount);
            return new PaymentResult(false, transactionId, "Simulated provider rejection");
        }

        log.info("[PayPal] Payment processed. TransactionId: {}, Amount: {}", transactionId, amount);
        return new PaymentResult(true, transactionId, null);
    }

    private PaymentResult paymentFallback(BigDecimal amount, Throwable t) {
        log.error("[PayPal] Circuit breaker open. Error: {}", t.getMessage());
        throw new PaymentServiceUnavailableException("paypal");
    }
}
