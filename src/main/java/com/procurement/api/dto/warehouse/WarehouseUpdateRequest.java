package com.procurement.api.dto.warehouse;

import jakarta.validation.constraints.Size;

public record WarehouseUpdateRequest(
        @Size(max = 32) String code,
        @Size(max = 255) String name,
        @Size(max = 255) String location,
        Boolean active
) {
}
