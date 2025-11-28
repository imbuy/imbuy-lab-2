package imbuy.lot.client;

import imbuy.lot.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallback = UserClientFallback.class, configuration = FeignConfig.class)
public interface UserClient {
    @CircuitBreaker(name = "userServiceClient")
    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}