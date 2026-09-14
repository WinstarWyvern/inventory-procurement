package com.procurement.api.dto.supplier;

import com.procurement.api.domain.Supplier;

import java.time.OffsetDateTime;

public record SupplierResponse(
        Long id, String name, String email, String phone, boolean active,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static SupplierResponse from(Supplier s) {
        if (s == null) return null;
        return new SupplierResponse(
                s.getId(), s.getName(), s.getEmail(), s.getPhone(), s.isActive(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
