package com.procurement.api.repository;

import com.procurement.api.domain.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Query("""
            SELECT m FROM InventoryMovement m
            WHERE (:warehouseId IS NULL OR m.warehouse.id = :warehouseId)
              AND (:productId IS NULL OR m.product.id = :productId)
            ORDER BY m.createdAt DESC
            """)
    List<InventoryMovement> search(Long warehouseId, Long productId);
}
