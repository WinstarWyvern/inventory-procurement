package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.dto.inventory.MovementResponse;
import com.procurement.api.dto.inventory.ProductStockResponse;
import com.procurement.api.dto.inventory.WarehouseStockResponse;
import com.procurement.api.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/warehouses/{warehouseId}/stock")
    public DataResponse<WarehouseStockResponse> stockByWarehouse(@PathVariable Long warehouseId) {
        return DataResponse.of(inventoryService.getStockByWarehouse(warehouseId));
    }

    @GetMapping("/products/{productId}/stock")
    public DataResponse<ProductStockResponse> stockByProduct(@PathVariable Long productId) {
        return DataResponse.of(inventoryService.getStockByProduct(productId));
    }

    @GetMapping("/movements")
    public DataResponse<List<MovementResponse>> movements(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId) {
        return DataResponse.of(inventoryService.getMovements(warehouseId, productId));
    }
}
