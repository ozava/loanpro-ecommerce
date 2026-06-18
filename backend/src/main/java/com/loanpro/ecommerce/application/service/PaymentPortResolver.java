package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.exception.UnsupportedPaymentMethodException;
import com.loanpro.ecommerce.application.port.out.PaymentPort;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentPortResolver {

    private final List<PaymentPort> paymentPorts;
    private Map<String, PaymentPort> portMap;

    public PaymentPortResolver(List<PaymentPort> paymentPorts) {
        this.paymentPorts = paymentPorts;
    }

    @PostConstruct
    void init() {
        portMap = new HashMap<>();
        for (PaymentPort port : paymentPorts) {
            portMap.put(port.getPaymentMethod(), port);
        }
    }

    public PaymentPort resolve(String paymentMethod) {
        PaymentPort port = portMap.get(paymentMethod);
        if (port == null) {
            throw new UnsupportedPaymentMethodException(paymentMethod, getSupportedMethods());
        }
        return port;
    }

    public Set<String> getSupportedMethods() {
        return portMap.keySet();
    }
}
