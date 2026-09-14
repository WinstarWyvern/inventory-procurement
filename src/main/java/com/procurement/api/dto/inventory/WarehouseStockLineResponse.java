package com.procurement.api.dto.inventory;

public record WarehouseStockLineResponse(
        Long warehouseId, String warehouseCode, String warehouseName, int quantity
) {
}
