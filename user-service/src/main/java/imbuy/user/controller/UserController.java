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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = userService.register(request);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<PageResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(userService.findAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(userService.updateProfile(id, request));
    }

    @PutMapping("/{id}/balance")
    @Operation(summary = "Update user balance")
    public ResponseEntity<UserDto> updateBalance(
            @PathVariable Long id,
            @RequestParam String balance) {

        return ResponseEntity.ok(userService.updateBalance(id, balance));
    }
}