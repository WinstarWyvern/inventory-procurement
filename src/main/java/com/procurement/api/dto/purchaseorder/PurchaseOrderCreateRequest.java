package com.procurement.api.dto.purchaseorder;

import jakarta.validation.constraints.NotNull;

public record PurchaseOrderCreateRequest(
        @NotNull(message = "purchaseRequestId is required") Long purchaseRequestId,
        @NotNull(message = "supplierId is required") Long supplierId
) {
}
