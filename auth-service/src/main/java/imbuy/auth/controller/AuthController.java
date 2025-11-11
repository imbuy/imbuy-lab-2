package imbuy.auth.controller;

import imbuy.auth.dto.AuthResponse;
import imbuy.auth.dto.LoginRequest;
import imbuy.auth.dto.RegisterRequest;
import imbuy.auth.dto.UserDto;
import imbuy.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate token")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        boolean isValid = authService.validateToken(extractToken(token));
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user from token")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserDto user = authService.getUserFromToken(extractToken(token));
        return ResponseEntity.ok(user);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}