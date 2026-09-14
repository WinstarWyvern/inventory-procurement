package com.procurement.api.dto.auth;

import com.procurement.api.dto.common.UserSummaryResponse;

public record LoginResponse(String token, UserSummaryResponse user) {
}
