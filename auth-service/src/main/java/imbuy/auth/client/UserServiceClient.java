package imbuy.auth.client;

import imbuy.auth.dto.RegisterRequest;
import imbuy.auth.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    @CircuitBreaker(name = "userServiceClient", fallbackMethod = "getUserByIdFallback")
    UserDto getUserById(@PathVariable Long id);

    @PostMapping("/users/register")
    @CircuitBreaker(name = "userServiceClient", fallbackMethod = "registerUserFallback")
    UserDto registerUser(@RequestBody RegisterRequest request);

    default UserDto getUserByIdFallback(Long id, Exception e) {
        throw new RuntimeException("User service unavailable: " + e.getMessage());
    }

    default UserDto registerUserFallback(RegisterRequest request, Exception e) {
        throw new RuntimeException("User service unavailable: " + e.getMessage());
    }
}