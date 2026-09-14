package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.InventoryBalance;
import com.procurement.api.domain.Product;
import com.procurement.api.domain.Warehouse;
import com.procurement.api.dto.inventory.*;
import com.procurement.api.dto.product.ProductResponse;
import com.procurement.api.dto.warehouse.WarehouseResponse;
import com.procurement.api.repository.InventoryBalanceRepository;
import com.procurement.api.repository.InventoryMovementRepository;
import com.procurement.api.repository.ProductRepository;
import com.procurement.api.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public InventoryService(
            InventoryBalanceRepository inventoryBalanceRepository,
            InventoryMovementRepository inventoryMovementRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    public WarehouseStockResponse getStockByWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> AppException.notFound("Warehouse"));

        List<StockLineResponse> lines = inventoryBalanceRepository
                .findByWarehouseIdOrderByProductNameAsc(warehouseId).stream()
                .map(this::toStockLine)
                .toList();

        return new WarehouseStockResponse(WarehouseResponse.from(warehouse), lines);
    }

    public ProductStockResponse getStockByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> AppException.notFound("Product"));

        List<WarehouseStockLineResponse> lines = inventoryBalanceRepository
                .findByProductIdOrderByWarehouseNameAsc(productId).stream()
                .map(this::toWarehouseStockLine)
                .toList();

        return new ProductStockResponse(ProductResponse.from(product), lines);
    }

    public List<MovementResponse> getMovements(Long warehouseId, Long productId) {
        return inventoryMovementRepository.search(warehouseId, productId).stream()
                .map(MovementResponse::from)
                .toList();
    }

    private StockLineResponse toStockLine(InventoryBalance balance) {
        Product product = balance.getProduct();
        return new StockLineResponse(
                product.getId(), product.getSku(), product.getName(), product.getUnit(), balance.getQuantity());
    }

    private WarehouseStockLineResponse toWarehouseStockLine(InventoryBalance balance) {
        Warehouse warehouse = balance.getWarehouse();
        return new WarehouseStockLineResponse(
                warehouse.getId(), warehouse.getCode(), warehouse.getName(), balance.getQuantity());
    }
}
