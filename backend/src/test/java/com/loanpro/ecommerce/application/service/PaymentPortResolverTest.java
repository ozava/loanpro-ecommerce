package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.exception.UnsupportedPaymentMethodException;
import com.loanpro.ecommerce.application.port.out.PaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPortResolverTest {

    @Mock
    private PaymentPort stripePort;

    @Mock
    private PaymentPort paypalPort;

    private PaymentPortResolver resolver;

    @BeforeEach
    void setUp() {
        when(stripePort.getPaymentMethod()).thenReturn("stripe");
        when(paypalPort.getPaymentMethod()).thenReturn("paypal");

        resolver = new PaymentPortResolver(List.of(stripePort, paypalPort));
        resolver.init();
    }

    @Test
    @DisplayName("resolve with lowercase method returns the correct payment port")
    void resolve_withLowercaseMethod_returnsCorrectPort() {
        PaymentPort result = resolver.resolve("stripe");

        assertThat(result).isSameAs(stripePort);
    }

    @Test
    @DisplayName("resolve with uppercase method returns the correct payment port (case-insensitive)")
    void resolve_withUppercaseMethod_returnsCorrectPort() {
        PaymentPort result = resolver.resolve("STRIPE");

        assertThat(result).isSameAs(stripePort);
    }

    @Test
    @DisplayName("resolve with mixed-case method returns the correct payment port")
    void resolve_withMixedCaseMethod_returnsCorrectPort() {
        PaymentPort result = resolver.resolve("StRiPe");

        assertThat(result).isSameAs(stripePort);
    }

    @Test
    @DisplayName("resolve with unsupported method throws UnsupportedPaymentMethodException")
    void resolve_withUnsupportedMethod_throwsUnsupportedPaymentMethodException() {
        assertThatThrownBy(() -> resolver.resolve("bitcoin"))
                .isInstanceOf(UnsupportedPaymentMethodException.class);
    }

    @Test
    @DisplayName("resolve with unsupported method lists all supported methods in exception message")
    void resolve_withUnsupportedMethod_exceptionListsSupportedMethods() {
        assertThatThrownBy(() -> resolver.resolve("bitcoin"))
                .isInstanceOf(UnsupportedPaymentMethodException.class)
                .hasMessageContaining("stripe")
                .hasMessageContaining("paypal");
    }
}
