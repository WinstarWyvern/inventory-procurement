package com.procurement.api.dto.purchaseorder;

import com.procurement.api.domain.PurchaseOrderItem;
import com.procurement.api.dto.product.ProductResponse;

public record PurchaseOrderItemResponse(
        Long id, ProductResponse product, int orderedQuantity, int receivedQuantity
) {
    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(), ProductResponse.from(item.getProduct()),
                item.getOrderedQuantity(), item.getReceivedQuantity());
    }
}
