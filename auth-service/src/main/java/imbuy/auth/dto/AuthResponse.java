package imbuy.auth.dto;

public record AuthResponse(
        String token,
        UserDto user
) {}
