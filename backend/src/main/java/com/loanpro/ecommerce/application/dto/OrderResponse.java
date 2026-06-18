package com.loanpro.ecommerce.application.dto;

import com.loanpro.ecommerce.domain.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String status,
        String paymentMethod,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt()
        );
    }
}
