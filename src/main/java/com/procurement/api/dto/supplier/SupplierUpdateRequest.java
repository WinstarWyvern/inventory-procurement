package com.procurement.api.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record SupplierUpdateRequest(
        @Size(max = 255) String name,
        @Email(message = "email must be a valid email address") @Size(max = 255) String email,
        @Size(max = 32) String phone,
        Boolean active
) {
}
