package com.procurement.api.dto.inventory;

import com.procurement.api.dto.warehouse.WarehouseResponse;

import java.util.List;

public record WarehouseStockResponse(WarehouseResponse warehouse, List<StockLineResponse> stock) {
}
