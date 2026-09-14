package com.procurement.api.dto.warehouse;

import com.procurement.api.domain.Warehouse;

import java.time.OffsetDateTime;

public record WarehouseResponse(
        Long id, String code, String name, String location, boolean active,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static WarehouseResponse from(Warehouse w) {
        if (w == null) return null;
        return new WarehouseResponse(
                w.getId(), w.getCode(), w.getName(), w.getLocation(), w.isActive(),
                w.getCreatedAt(), w.getUpdatedAt());
    }
}
