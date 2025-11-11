package imbuy.auth.service;

import imbuy.auth.client.UserServiceClient;
import imbuy.auth.dto.AuthResponse;
import imbuy.auth.dto.LoginRequest;
import imbuy.auth.dto.RegisterRequest;
import imbuy.auth.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserServiceClient userServiceClient;

    public AuthResponse register(RegisterRequest request) {
        try {
            UserDto user = userServiceClient.registerUser(request);

            String token = generateToken(user);

            return new AuthResponse(token, user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        UserDto user = new UserDto(1L, request.email(), "user", "0.00");

        String token = generateToken(user);
        return new AuthResponse(token, user);
    }

    public boolean validateToken(String token) {
        return token != null && !token.trim().isEmpty();
    }

    public UserDto getUserFromToken(String token) {
        if (!validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        return new UserDto(1L, "user@example.com", "demo_user", "100.00");
    }

    private String generateToken(UserDto user) {
        return "token-" + user.id() + "-" + UUID.randomUUID();
    }
}