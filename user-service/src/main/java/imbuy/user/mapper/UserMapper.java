package imbuy.user.mapper;

import imbuy.user.domain.User;
import imbuy.user.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto mapToDto(User user);
}