package com.procurement.api.security;

import com.procurement.api.domain.UserRole;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the {@code Authorization: Bearer <token>} header on every request
 * and, if valid, stores an {@link AuthenticatedUser} in the security
 * context for downstream controllers/services to use.
 *
 * Invalid or missing tokens are simply not authenticated here - Spring
 * Security's {@code authorizeHttpRequests} rules (see {@code SecurityConfig})
 * are what actually reject the request with 401/403.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            try {
                var claims = jwtService.parseClaims(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                UserRole role = UserRole.valueOf(claims.get("role", String.class));

                AuthenticatedUser principal = new AuthenticatedUser(userId, username, role);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired token: leave the context unauthenticated so
                // authorizeHttpRequests rejects the request with 401.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
