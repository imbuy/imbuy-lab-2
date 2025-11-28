package imbuy.lot.client;

import imbuy.lot.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserDto getUserById(Long id) {
        return new UserDto(
                id,
                "unknown@example.com",
                "Unknown (fallback)"
        );
    }
}