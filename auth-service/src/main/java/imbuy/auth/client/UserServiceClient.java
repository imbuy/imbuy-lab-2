package imbuy.auth.client;

import imbuy.auth.dto.RegisterRequest;
import imbuy.auth.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable Long id);

    @PostMapping("/users/register")
    UserDto registerUser(@RequestBody RegisterRequest request);
}