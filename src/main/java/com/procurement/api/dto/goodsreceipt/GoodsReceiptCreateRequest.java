package com.procurement.api.dto.goodsreceipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GoodsReceiptCreateRequest(
        @NotNull(message = "purchaseOrderId is required") Long purchaseOrderId,
        @NotEmpty(message = "items must contain at least one item") @Valid List<GoodsReceiptItemInput> items
) {
}
