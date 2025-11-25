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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Mono<UserDto> register(RegisterRequest request) {
        return Mono.fromCallable(() -> userRepository.existsByEmail(request.email()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));
                    }
                    User user = User.builder()
                            .email(request.email())
                            .password(request.password())
                            .username(request.username())
                            .build();

                    return Mono.fromCallable(() -> userRepository.save(user))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(userMapper::mapToDto);
                });
    }

    public Mono<UserDto> updateProfile(Long userId, RegisterRequest request) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userOpt -> {
                    if (userOpt.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    }

                    User user = userOpt.get();
                    User.UserBuilder userBuilder = user.toBuilder()
                            .username(request.username());

                    if (request.password() != null && !request.password().isEmpty()) {
                        userBuilder.password(request.password());
                    }

                    User updatedUser = userBuilder.build();

                    return Mono.fromCallable(() -> userRepository.save(updatedUser))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(userMapper::mapToDto);
                });
    }

    public Mono<PageResponse<UserDto>> findAllUsers(Pageable pageable) {
        return Mono.fromCallable(() -> userRepository.findAll(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(page -> PageResponse.of(page.map(userMapper::mapToDto)));
    }

    public Mono<UserDto> findById(Long id) {
        return Mono.fromCallable(() -> userRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userOpt -> userOpt.map(user -> Mono.just(userMapper.mapToDto(user))).orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))));
    }

    public Mono<User> getUserEntityById(Long id) {
        return Mono.fromCallable(() -> userRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(userOpt -> userOpt.<Mono<? extends User>>map(Mono::just).orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))));
    }

    public Mono<UserDto> updateBalance(Long userId, String balance) {
        return getUserEntityById(userId)
                .map(user -> user.toBuilder()
                        .balance(new BigDecimal(balance))
                        .build())
                .flatMap(updatedUser -> Mono.fromCallable(() -> userRepository.save(updatedUser))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(userMapper::mapToDto);
    }
}