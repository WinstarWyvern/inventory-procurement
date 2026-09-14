package com.procurement.api.dto.purchaserequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequestItemInput(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required") @Positive(message = "quantity must be greater than 0") Integer quantity
) {
}
