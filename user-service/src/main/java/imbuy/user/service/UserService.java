package imbuy.user.service;

import imbuy.user.domain.User;
import imbuy.user.dto.RegisterRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.mapper.UserMapper;
import imbuy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
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
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Email already exists"));
                    }

                    User user = User.builder()
                            .email(request.email())
                            .password(request.password())
                            .username(request.username())
                            .build();

                    return saveUser(user);
                })
                .doOnSuccess(user -> log.info("User registered: id={}, email={}",
                        user.id(), user.email()));
    }

    public Flux<UserDto> findAllUsers(Pageable pageable) {
        return Mono.fromCallable(() -> userRepository.findAll(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(page -> page.getContent())
                .map(userMapper::mapToDto);
    }

    public Mono<UserDto> findById(Long id) {
        return findUserById(id)
                .map(userMapper::mapToDto)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found")));
    }

    private Mono<User> findUserById(Long id) {
        return Mono.fromCallable(() -> userRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional.map(Mono::just)
                        .orElse(Mono.empty()));
    }

    private Mono<UserDto> saveUser(User user) {
        return Mono.fromCallable(() -> userRepository.save(user))
                .subscribeOn(Schedulers.boundedElastic())
                .map(userMapper::mapToDto);
    }
}