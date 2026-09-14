package com.procurement.api.dto.common;

import com.procurement.api.domain.User;
import com.procurement.api.domain.UserRole;

public record UserSummaryResponse(Long id, String username, String name, UserRole role) {
    public static UserSummaryResponse from(User user) {
        if (user == null) return null;
        return new UserSummaryResponse(user.getId(), user.getUsername(), user.getName(), user.getRole());
    }
}
