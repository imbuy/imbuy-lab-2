package imbuy.user.mapper;

import imbuy.user.domain.User;
import imbuy.user.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername()
        );
    }
}