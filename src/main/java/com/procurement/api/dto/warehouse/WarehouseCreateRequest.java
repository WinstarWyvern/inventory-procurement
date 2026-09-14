package com.procurement.api.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseCreateRequest(
        @NotBlank(message = "code is required") @Size(max = 32) String code,
        @NotBlank(message = "name is required") @Size(max = 255) String name,
        @Size(max = 255) String location,
        Boolean active
) {
}
