package com.procurement.api.security;

import com.procurement.api.domain.UserRole;

/** The authenticated principal extracted from a validated JWT. */
public record AuthenticatedUser(Long id, String username, UserRole role) {
}
