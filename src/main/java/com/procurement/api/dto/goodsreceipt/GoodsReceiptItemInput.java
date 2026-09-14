package com.procurement.api.dto.goodsreceipt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GoodsReceiptItemInput(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required") @Positive(message = "quantity must be greater than 0") Integer quantity
) {
}
