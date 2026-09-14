package com.procurement.api.dto.inventory;

import com.procurement.api.dto.product.ProductResponse;

import java.util.List;

public record ProductStockResponse(ProductResponse product, List<WarehouseStockLineResponse> stock) {
}
