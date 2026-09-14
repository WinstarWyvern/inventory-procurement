package com.procurement.api.dto.purchaserequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PurchaseRequestCreateRequest(
        @NotNull(message = "warehouseId is required") Long warehouseId,
        @Valid List<PurchaseRequestItemInput> items
) {
    public List<PurchaseRequestItemInput> itemsOrEmpty() {
        return items == null ? List.of() : items;
    }
}
