package imbuy.user.controller;

import imbuy.user.dto.PageResponse;
import imbuy.user.dto.RegisterRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public Mono<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public Mono<PageResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return userService.findAllUsers(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public Mono<UserDto> getUserById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update user profile")
    public Mono<UserDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody RegisterRequest request) {

        return userService.updateProfile(id, request);
    }

    @PutMapping("/{id}/balance")
    @Operation(summary = "Update user balance")
    public Mono<UserDto> updateBalance(
            @PathVariable Long id,
            @RequestParam String balance) {

        return userService.updateBalance(id, balance);
    }
}