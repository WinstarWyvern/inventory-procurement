package com.procurement.api.dto.purchaserequest;

import com.procurement.api.domain.PurchaseRequestItem;
import com.procurement.api.dto.product.ProductResponse;

public record PurchaseRequestItemResponse(Long id, ProductResponse product, int quantity) {
    public static PurchaseRequestItemResponse from(PurchaseRequestItem item) {
        return new PurchaseRequestItemResponse(
                item.getId(), ProductResponse.from(item.getProduct()), item.getQuantity());
    }
}
