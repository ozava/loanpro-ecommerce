package com.loanpro.ecommerce.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Pattern(regexp = "^[^<>]*$", message = "must not contain HTML tags") String name,
        @NotBlank @Pattern(regexp = "^[^<>]*$", message = "must not contain HTML tags") String sku,
        @Pattern(regexp = "^[^<>]*$", message = "must not contain HTML tags") String description,
        @Pattern(regexp = "^[^<>]*$", message = "must not contain HTML tags") String categoryName,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        BigDecimal weightKg
) {}
