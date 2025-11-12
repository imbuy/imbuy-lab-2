package imbuy.auth.client;

import imbuy.auth.dto.RegisterRequest;
import imbuy.auth.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserDto getUserById(Long id) {
        throw new RuntimeException("User service is currently unavailable. Please try again later.");
    }

    @Override
    public UserDto registerUser(RegisterRequest request) {
        throw new RuntimeException("User service is currently unavailable. Please try again later.");
    }
}

