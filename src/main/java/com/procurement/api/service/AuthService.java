package com.procurement.api.service;

import com.procurement.api.common.AppException;
import com.procurement.api.domain.User;
import com.procurement.api.dto.auth.LoginRequest;
import com.procurement.api.dto.auth.LoginResponse;
import com.procurement.api.dto.common.UserSummaryResponse;
import com.procurement.api.repository.UserRepository;
import com.procurement.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(AppException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AppException.invalidCredentials();
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserSummaryResponse.from(user));
    }

    public UserSummaryResponse getById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> AppException.notFound("User"));
        return UserSummaryResponse.from(user);
    }
}
