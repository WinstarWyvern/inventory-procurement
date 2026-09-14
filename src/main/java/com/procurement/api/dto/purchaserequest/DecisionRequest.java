package com.procurement.api.dto.purchaserequest;

import jakarta.validation.constraints.Size;

public record DecisionRequest(@Size(max = 1000) String remarks) {
}
