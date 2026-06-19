package com.loanpro.ecommerce.application.dto;

import com.loanpro.ecommerce.domain.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        Integer stock,
        BigDecimal weightKg,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getPrice(),
                product.getStock(),
                product.getWeightKg(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}