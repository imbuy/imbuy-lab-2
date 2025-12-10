package imbuy.user;

import imbuy.user.domain.User;
import imbuy.user.dto.PageResponse;
import imbuy.user.dto.RegisterRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.repository.UserRepository;
import imbuy.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=true",
        "spring.datasource.url=jdbc:postgresql://localhost:${testcontainers.postgresql.port}/user_test",
        "spring.datasource.username=test",
        "spring.datasource.password=test"
})
class UserServiceApplicationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("user_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("testcontainers.postgresql.port", postgres::getFirstMappedPort);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private RegisterRequest validRequest;
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";
    private final String testUsername = "testuser";

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();

        validRequest = new RegisterRequest(
                testEmail,
                testPassword,
                testUsername
        );
    }

    @Test
    void register_shouldCreateNewUser() {
        StepVerifier.create(userService.register(validRequest))
                .assertNext(userDto -> {
                    assertNotNull(userDto);
                    assertNotNull(userDto.id());
                    assertEquals(testEmail, userDto.email());
                    assertEquals(testUsername, userDto.username());
                })
                .verifyComplete();

        List<User> allUsers = userRepository.findAll();
        Optional<User> savedUser = allUsers.stream()
                .filter(user -> testEmail.equals(user.getEmail()))
                .findFirst();

        assertTrue(savedUser.isPresent());
        assertEquals(testUsername, savedUser.get().getUsername());
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        User existingUser = User.builder()
                .email(testEmail)
                .password(testPassword)
                .username(testUsername)
                .build();
        userRepository.save(existingUser);

        StepVerifier.create(userService.register(validRequest))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Email already exists"))
                .verify();
    }
    @Test
    void findById_shouldReturnUserWhenExists() {
        User savedUser = userRepository.save(
                User.builder()
                        .email(testEmail)
                        .password(testPassword)
                        .username(testUsername)
                        .build()
        );

        StepVerifier.create(userService.findById(savedUser.getId()))
                .assertNext(userDto -> {
                    assertNotNull(userDto);
                    assertEquals(savedUser.getId(), userDto.id());
                    assertEquals(testEmail, userDto.email());
                    assertEquals(testUsername, userDto.username());
                })
                .verifyComplete();
    }

    @Test
    void findById_shouldThrowWhenUserNotFound() {
        StepVerifier.create(userService.findById(999L))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("User not found"))
                .verify();
    }

    @Test
    void findAllUsers_shouldReturnPaginatedResults() {
        for (int i = 1; i <= 15; i++) {
            userRepository.save(
                    User.builder()
                            .email("user" + i + "@example.com")
                            .password("password" + i)
                            .username("username" + i)
                            .build()
            );
        }

        StepVerifier.create(userService.findAllUsers(PageRequest.of(0, 10)))
                .assertNext(pageResponse -> {
                    assertNotNull(pageResponse);
                    assertEquals(10, pageResponse.content().size());
                    assertEquals(0, pageResponse.current_page());
                    assertEquals(10, pageResponse.page_size());
                    assertTrue(pageResponse.has_next());
                    assertFalse(pageResponse.has_previous());
                })
                .verifyComplete();

        StepVerifier.create(userService.findAllUsers(PageRequest.of(1, 10)))
                .assertNext(pageResponse -> {
                    assertNotNull(pageResponse);
                    assertEquals(5, pageResponse.content().size());
                    assertEquals(1, pageResponse.current_page());
                    assertEquals(10, pageResponse.page_size());
                    assertFalse(pageResponse.has_next());
                    assertTrue(pageResponse.has_previous());
                })
                .verifyComplete();
    }

    @Test
    void updateProfile_shouldUpdateUserFields() {
        User savedUser = userRepository.save(
                User.builder()
                        .email(testEmail)
                        .password(testPassword)
                        .username("oldusername")
                        .build()
        );

        RegisterRequest updateRequest = new RegisterRequest(
                testEmail,
                "newpassword123",
                "newusername"
        );

        StepVerifier.create(userService.updateProfile(savedUser.getId(), updateRequest))
                .assertNext(updatedUser -> {
                    assertNotNull(updatedUser);
                    assertEquals(savedUser.getId(), updatedUser.id());
                    assertEquals("newusername", updatedUser.username());
                })
                .verifyComplete();

        Optional<User> userInDb = userRepository.findById(savedUser.getId());
        assertTrue(userInDb.isPresent());
        assertEquals("newusername", userInDb.get().getUsername());
    }

    @Test
    void updateProfile_shouldUpdateOnlyUsernameWhenPasswordIsNull() {
        User savedUser = userRepository.save(
                User.builder()
                        .email(testEmail)
                        .password(testPassword)
                        .username("oldusername")
                        .build()
        );

        RegisterRequest updateRequest = new RegisterRequest(
                testEmail,
                null,
                "newusername"
        );

        StepVerifier.create(userService.updateProfile(savedUser.getId(), updateRequest))
                .assertNext(updatedUser -> {
                    assertEquals("newusername", updatedUser.username());
                })
                .verifyComplete();
    }

    @Test
    void updateProfile_shouldThrowWhenUserNotFound() {
        RegisterRequest updateRequest = new RegisterRequest(
                "nonexistent@example.com",
                "newpassword",
                "newusername"
        );

        StepVerifier.create(userService.updateProfile(999L, updateRequest))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("User not found"))
                .verify();
    }

    @Test
    void updateProfile_shouldUpdatePasswordWhenProvided() {
        User savedUser = userRepository.save(
                User.builder()
                        .email(testEmail)
                        .password("oldpassword")
                        .username(testUsername)
                        .build()
        );

        RegisterRequest updateRequest = new RegisterRequest(
                testEmail,
                "brandnewpassword",
                testUsername
        );

        StepVerifier.create(userService.updateProfile(savedUser.getId(), updateRequest))
                .assertNext(updatedUser -> {
                    assertNotNull(updatedUser);
                    assertEquals(testUsername, updatedUser.username());
                })
                .verifyComplete();

        Optional<User> userInDb = userRepository.findById(savedUser.getId());
        assertTrue(userInDb.isPresent());
        assertEquals("brandnewpassword", userInDb.get().getPassword());
    }

    @Test
    void repository_existsByEmail_shouldWork() {
        userRepository.save(
                User.builder()
                        .email(testEmail)
                        .password(testPassword)
                        .username(testUsername)
                        .build()
        );

        assertTrue(userRepository.existsByEmail(testEmail));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void completeUserFlow_shouldWork() {
        StepVerifier.create(userService.register(validRequest))
                .assertNext(registeredUser -> {
                    assertNotNull(registeredUser.id());
                    assertEquals(testEmail, registeredUser.email());
                })
                .verifyComplete();

        List<User> allUsers = userRepository.findAll();
        Optional<User> userInDb = allUsers.stream()
                .filter(user -> testEmail.equals(user.getEmail()))
                .findFirst();
        assertTrue(userInDb.isPresent());
        Long userId = userInDb.get().getId();

        StepVerifier.create(userService.findById(userId))
                .assertNext(foundUser -> {
                    assertEquals(testEmail, foundUser.email());
                    assertEquals(testUsername, foundUser.username());
                })
                .verifyComplete();

        RegisterRequest updateRequest = new RegisterRequest(
                testEmail,
                "updatedpassword",
                "updatedusername"
        );

        StepVerifier.create(userService.updateProfile(userId, updateRequest))
                .assertNext(updatedUser -> {
                    assertEquals("updatedusername", updatedUser.username());
                })
                .verifyComplete();

        StepVerifier.create(userService.findAllUsers(PageRequest.of(0, 10)))
                .assertNext(pageResponse -> {
                    assertEquals(1, pageResponse.content().size());
                    assertEquals("updatedusername", pageResponse.content().get(0).username());
                })
                .verifyComplete();
    }

    @Test
    void userDto_creation_shouldWork() {
        UserDto dto = new UserDto(
                1L,
                "user@example.com",
                "testuser"
        );

        assertEquals(1L, dto.id());
        assertEquals("user@example.com", dto.email());
        assertEquals("testuser", dto.username());
    }

    @Test
    void registerRequest_validation_shouldWork() {
        RegisterRequest valid = new RegisterRequest(
                "valid@example.com",
                "password",
                "username"
        );
        assertEquals("valid@example.com", valid.email());
        assertEquals("password", valid.password());
        assertEquals("username", valid.username());

        RegisterRequest withNulls = new RegisterRequest(
                null,
                null,
                null
        );
        assertNull(withNulls.email());
        assertNull(withNulls.password());
        assertNull(withNulls.username());
    }

    @Test
    void pageResponse_creation_shouldWork() {
        UserDto user1 = new UserDto(1L, "user1@example.com", "user1");
        UserDto user2 = new UserDto(2L, "user2@example.com", "user2");

        PageResponse<UserDto> response = new PageResponse<>(
                List.of(user1, user2),
                0,
                10,
                true,
                false
        );

        assertEquals(2, response.content().size());
        assertEquals(0, response.current_page());
        assertEquals(10, response.page_size());
        assertTrue(response.has_next());
        assertFalse(response.has_previous());
    }

    @Test
    void concurrentRegistration_shouldHandleGracefully() {
        int numberOfThreads = 5;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(numberOfThreads);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numberOfThreads);
        java.util.List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    RegisterRequest request = new RegisterRequest(
                            "concurrent" + threadNum + "@example.com",
                            "password" + threadNum,
                            "user" + threadNum
                    );

                    userService.register(request)
                            .block(Duration.ofSeconds(5));

                } catch (Throwable e) {
                    exceptions.add(e);
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<User> allUsers = userRepository.findAll();
        long successfulRegistrations = java.util.stream.IntStream.range(0, numberOfThreads)
                .filter(i -> allUsers.stream()
                        .anyMatch(user -> ("concurrent" + i + "@example.com").equals(user.getEmail())))
                .count();

        assertTrue(successfulRegistrations <= numberOfThreads);
        assertTrue(successfulRegistrations > 0);
    }

    @Test
    void userEntity_builder_shouldWork() {
        User user = User.builder()
                .email("builder@example.com")
                .password("builderpass")
                .username("builderuser")
                .build();

        assertEquals("builder@example.com", user.getEmail());
        assertEquals("builderpass", user.getPassword());
        assertEquals("builderuser", user.getUsername());
        assertNull(user.getId());
        assertNull(user.getCreatedAt());
    }

    @Test
    void userEntity_toBuilder_shouldWork() {
        User original = User.builder()
                .email("original@example.com")
                .password("originalpass")
                .username("originaluser")
                .build();

        User modified = original.toBuilder()
                .username("modifieduser")
                .build();

        assertEquals("original@example.com", modified.getEmail());
        assertEquals("originalpass", modified.getPassword());
        assertEquals("modifieduser", modified.getUsername());
    }

    @Test
    void repository_saveAndFindAll_shouldWork() {
        User user1 = User.builder()
                .email("user1@example.com")
                .password("pass1")
                .username("user1")
                .build();

        User user2 = User.builder()
                .email("user2@example.com")
                .password("pass2")
                .username("user2")
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> allUsers = userRepository.findAll();
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(u -> "user1".equals(u.getUsername())));
        assertTrue(allUsers.stream().anyMatch(u -> "user2".equals(u.getUsername())));
    }

    @Test
    void repository_findById_shouldWork() {
        User savedUser = userRepository.save(
                User.builder()
                        .email("findbyid@example.com")
                        .password("password")
                        .username("findbyiduser")
                        .build()
        );

        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("findbyid@example.com", foundUser.get().getEmail());
    }

    @Test
    void repository_deleteById_shouldWork() {
        User savedUser = userRepository.save(
                User.builder()
                        .email("todelete@example.com")
                        .password("password")
                        .username("todelete")
                        .build()
        );

        userRepository.deleteById(savedUser.getId());

        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }
}