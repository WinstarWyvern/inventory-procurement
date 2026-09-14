package com.procurement.api.dto.purchaserequest;

import jakarta.validation.constraints.NotNull;

public record PurchaseRequestWarehouseUpdateRequest(
        @NotNull(message = "warehouseId is required") Long warehouseId
) {
}
