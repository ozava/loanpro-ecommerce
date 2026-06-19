package com.loanpro.ecommerce.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        String description,
        String categoryName,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        BigDecimal weightKg
) {}