package com.procurement.api.dto.product;

import jakarta.validation.constraints.Size;

/** All fields optional - only non-null fields are applied. */
public record ProductUpdateRequest(
        @Size(max = 64) String sku,
        @Size(max = 255) String name,
        @Size(max = 32) String unit,
        Boolean active
) {
}
