package com.procurement.api.repository;

import com.procurement.api.domain.InventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    Optional<InventoryBalance> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<InventoryBalance> findByWarehouseIdOrderByProductNameAsc(Long warehouseId);

    List<InventoryBalance> findByProductIdOrderByWarehouseNameAsc(Long productId);
}
