package com.procurement.api.dto.inventory;

import com.procurement.api.domain.InventoryMovement;
import com.procurement.api.domain.InventoryMovementType;

import java.time.OffsetDateTime;

public record MovementResponse(
        Long id, Long warehouseId, Long productId, InventoryMovementType movementType,
        int quantity, String reference, OffsetDateTime createdAt
) {
    public static MovementResponse from(InventoryMovement m) {
        return new MovementResponse(
                m.getId(), m.getWarehouse().getId(), m.getProduct().getId(), m.getMovementType(),
                m.getQuantity(), m.getReference(), m.getCreatedAt());
    }
}
