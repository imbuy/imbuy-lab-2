package imbuy.user.service;

import imbuy.user.domain.User;
import imbuy.user.dto.PageResponse;
import imbuy.user.dto.RegisterRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.mapper.UserMapper;
import imbuy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setUsername(request.username());
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return userMapper.mapToDto(savedUser);
    }

    public UserDto updateProfile(Long userId, RegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setUsername(request.username());
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(request.password());
        }

        User updatedUser = userRepository.save(user);
        return userMapper.mapToDto(updatedUser);
    }

    public PageResponse<UserDto> findAllUsers(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable).map(userMapper::mapToDto));
    }

    public UserDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return userMapper.mapToDto(user);
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDto updateBalance(Long userId, String balance) {
        User user = getUserEntityById(userId);
        user.setBalance(new java.math.BigDecimal(balance));
        User updatedUser = userRepository.save(user);
        return userMapper.mapToDto(updatedUser);
    }
}