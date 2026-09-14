package com.procurement.api.web;

import com.procurement.api.common.DataResponse;
import com.procurement.api.dto.auth.LoginRequest;
import com.procurement.api.dto.auth.LoginResponse;
import com.procurement.api.dto.common.UserSummaryResponse;
import com.procurement.api.security.AuthenticatedUser;
import com.procurement.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public DataResponse<UserSummaryResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return DataResponse.of(authService.getById(principal.id()));
    }
}
