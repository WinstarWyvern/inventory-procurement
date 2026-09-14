package com.procurement.api.dto.inventory;

public record StockLineResponse(
        Long productId, String sku, String productName, String unit, int quantity
) {
}
