package com.procurement.api.dto.product;

import com.procurement.api.domain.Product;

import java.time.OffsetDateTime;

public record ProductResponse(
        Long id, String sku, String name, String unit, boolean active,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        if (p == null) return null;
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getUnit(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
