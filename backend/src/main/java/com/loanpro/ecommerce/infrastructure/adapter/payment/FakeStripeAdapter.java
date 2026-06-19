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
public class FakeStripeAdapter implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(FakeStripeAdapter.class);
    private final SecureRandom random = new SecureRandom();

    @Override
    public String getPaymentMethod() {
        return "stripe";
    }

    @Override
    @CircuitBreaker(name = "stripe", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        boolean failure = random.nextInt(100) < 10;

        if (failure) {
            log.warn("[Stripe] Payment declined. TransactionId: {}, Amount: {}, Reason: Simulated provider rejection",
                    transactionId, amount);
            return new PaymentResult(false, transactionId, "Simulated provider rejection");
        }

        log.info("[Stripe] Payment processed. TransactionId: {}, Amount: {}", transactionId, amount);
        return new PaymentResult(true, transactionId, null);
    }

    private PaymentResult paymentFallback(BigDecimal amount, Throwable t) {
        log.error("[Stripe] Circuit breaker open. Error: {}", t.getMessage());
        throw new PaymentServiceUnavailableException("stripe");
    }
}
