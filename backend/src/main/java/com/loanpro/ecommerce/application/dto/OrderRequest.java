package com.loanpro.ecommerce.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotEmpty List<@Valid OrderItemRequest> items,
        @NotBlank String paymentMethod
) {
}
