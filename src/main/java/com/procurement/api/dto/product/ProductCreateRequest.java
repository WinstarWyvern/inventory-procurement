package com.procurement.api.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank(message = "sku is required") @Size(max = 64) String sku,
        @NotBlank(message = "name is required") @Size(max = 255) String name,
        @NotBlank(message = "unit is required") @Size(max = 32) String unit,
        Boolean active
) {
}
