package imbuy.lot;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import imbuy.lot.client.BidClient;
import imbuy.lot.client.UserClient;
import imbuy.lot.domain.Lot;
import imbuy.lot.dto.*;
import imbuy.lot.enums.LotStatus;
import imbuy.lot.repository.LotRepository;
import imbuy.lot.service.LotService;
import imbuy.lot.service.LotScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "user-service.url=http://localhost:${wiremock.server.port}",
        "bid-service.url=http://localhost:${wiremock.server.port}"
})
class LotServiceApplicationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("lot_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.server.port", wireMock::getPort);
    }

    @Autowired
    private LotService lotService;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotScheduler lotScheduler;

    @MockBean
    private UserClient userClient;

    @MockBean
    private BidClient bidClient;

    private Lot testLot;
    private UserDto testUser;
    private final Long testOwnerId = 1L;

    @BeforeEach
    void beforeEach() {
        lotRepository.deleteAll();

        wireMock.resetAll();

        testUser = new UserDto(testOwnerId, "owner@test.com", "testuser");

        testLot = Lot.builder()
                .title("Test Laptop")
                .description("High performance laptop")
                .startPrice(new BigDecimal("1000.00"))
                .currentPrice(new BigDecimal("1000.00"))
                .bidStep(new BigDecimal("50.00"))
                .ownerId(testOwnerId)
                .categoryId(1L)
                .status(LotStatus.DRAFT)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        testLot = lotRepository.save(testLot);

        when(userClient.getUserById(testOwnerId)).thenReturn(testUser);
        when(userClient.getUserById(2L)).thenReturn(new UserDto(2L, "user2@test.com", "user2"));
        when(userClient.getUserById(3L)).thenReturn(new UserDto(3L, "winner@test.com", "winner"));
    }

    @Test
    void getLots_shouldReturnFilteredResults() {
        LotFilterDto filter = new LotFilterDto(null, null, null, null, false);

        PageResponse<LotDto> result = lotService.getLots(filter,
                PageRequest.of(0, 10), testOwnerId);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Test Laptop", result.content().get(0).title());
        assertEquals("testuser", result.content().get(0).owner_username());
    }

    @Test
    void getLots_withTitleFilter_shouldWork() {
        LotFilterDto filter = new LotFilterDto("Laptop", null, null, null, false);

        PageResponse<LotDto> result = lotService.getLots(filter,
                PageRequest.of(0, 10), testOwnerId);

        assertEquals(1, result.content().size());
    }

    @Test
    void getLotById_shouldReturnWithUserInfo() {
        LotDto result = lotService.getLotById(testLot.getId());

        assertNotNull(result);
        assertEquals(testLot.getId(), result.id());
        assertEquals("Test Laptop", result.title());
        assertEquals("testuser", result.owner_username());
        assertEquals(LotStatus.DRAFT, result.status());
    }

    @Test
    void getLotById_shouldThrowWhenNotFound() {
        assertThrows(RuntimeException.class, () ->
                lotService.getLotById(999L));
    }

    @Test
    void createLot_shouldCreateNewLot() {
        CreateLotDto request = new CreateLotDto(
                "New Laptop",
                "Brand new laptop",
                new BigDecimal("1500.00"),
                new BigDecimal("100.00"),
                2L,
                testOwnerId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(14)
        );

        LotDto result = lotService.createLot(request, testOwnerId);

        assertNotNull(result);
        assertEquals("New Laptop", result.title());
        assertEquals(new BigDecimal("1500.00"), result.start_price());
        assertEquals(LotStatus.PENDING_APPROVAL, result.status());
        assertEquals("testuser", result.owner_username());
    }

    @Test
    void createLot_withPastEndDate_shouldThrow() {
        CreateLotDto request = new CreateLotDto(
                "Invalid Lot",
                "Description",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                1L,
                testOwnerId,
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(1)
        );

        assertThrows(RuntimeException.class, () ->
                lotService.createLot(request, testOwnerId));
    }

    @Test
    void approveLot_shouldChangeStatusToActive() {
        testLot = testLot.toBuilder()
                .status(LotStatus.PENDING_APPROVAL)
                .build();
        testLot = lotRepository.save(testLot);

        LotDto result = lotService.approveLot(testLot.getId(), testOwnerId);

        assertEquals(LotStatus.ACTIVE, result.status());

        Lot updated = lotRepository.findById(testLot.getId()).orElseThrow();
        assertEquals(LotStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void approveLot_shouldThrowWhenNotOwner() {
        testLot = testLot.toBuilder()
                .status(LotStatus.PENDING_APPROVAL)
                .build();
        testLot = lotRepository.save(testLot);

        assertThrows(RuntimeException.class, () ->
                lotService.approveLot(testLot.getId(), 999L));
    }

    @Test
    void cancelLot_shouldChangeStatusToCancelled() {
        testLot = testLot.toBuilder()
                .status(LotStatus.PENDING_APPROVAL)
                .build();
        testLot = lotRepository.save(testLot);

        LotDto result = lotService.cancelLot(testLot.getId(), testOwnerId, "Changed my mind");

        assertEquals(LotStatus.CANCELLED, result.status());
    }

    @Test
    void updateLot_shouldUpdateFields() {
        UpdateLotDto request = new UpdateLotDto(
                "Updated Title",
                "Updated Description",
                new BigDecimal("75.00"),
                2L,
                LocalDateTime.now().plusDays(21)
        );

        LotDto result = lotService.updateLot(testLot.getId(), request, testOwnerId);

        assertEquals("Updated Title", result.title());
        assertEquals("Updated Description", result.description());
        assertEquals(new BigDecimal("75.00"), result.bid_step());
        assertEquals(2L, result.category_id());
    }

    @Test
    void updateLot_shouldThrowWhenActive() {
        testLot = testLot.toBuilder()
                .status(LotStatus.ACTIVE)
                .build();
        testLot = lotRepository.save(testLot);

        UpdateLotDto request = new UpdateLotDto(
                "New Title", null, null, null, null
        );

        assertThrows(RuntimeException.class, () ->
                lotService.updateLot(testLot.getId(), request, testOwnerId));
    }

    @Test
    void deleteLot_shouldDeleteWhenDraft() {
        lotService.deleteLot(testLot.getId(), testOwnerId);

        Optional<Lot> deleted = lotRepository.findById(testLot.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    void deleteLot_shouldThrowWhenActive() {
        testLot = testLot.toBuilder()
                .status(LotStatus.ACTIVE)
                .build();
        testLot = lotRepository.save(testLot);

        assertThrows(RuntimeException.class, () ->
                lotService.deleteLot(testLot.getId(), testOwnerId));
    }

    @Test
    void getLots_withUserServiceUnavailable_shouldUseFallback() {
        when(userClient.getUserById(testOwnerId)).thenThrow(new RuntimeException("Service unavailable"));

        LotFilterDto filter = new LotFilterDto(null, null, null, null, false);

        PageResponse<LotDto> result = lotService.getLots(filter,
                PageRequest.of(0, 10), testOwnerId);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("User Service Unavailable", result.content().get(0).owner_username());
    }

    @Test
    void getLotById_withUserServiceUnavailable_shouldUseFallback() {
        when(userClient.getUserById(testOwnerId)).thenThrow(new RuntimeException("Service unavailable"));

        LotDto result = lotService.getLotById(testLot.getId());

        assertNotNull(result);
        assertEquals("User Service Unavailable", result.owner_username());
    }

    @Test
    void closeExpiredLots_shouldCloseActiveExpiredLot() {
        Lot expiredLot = Lot.builder()
                .title("Expired Lot")
                .description("Should be closed")
                .startPrice(new BigDecimal("500.00"))
                .currentPrice(new BigDecimal("500.00"))
                .bidStep(new BigDecimal("25.00"))
                .ownerId(testOwnerId)
                .status(LotStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .build();

        expiredLot = lotRepository.save(expiredLot);

        when(bidClient.getAuctionWinner(expiredLot.getId())).thenReturn(3L);

        lotScheduler.closeExpiredLots();

        Lot closed = lotRepository.findById(expiredLot.getId()).orElseThrow();
        assertEquals(LotStatus.COMPLETED, closed.getStatus());
        assertEquals(3L, closed.getWinnerId());
    }

    @Test
    void closeExpiredLots_withNoBids_shouldCloseWithoutWinner() {
        Lot expiredLot = Lot.builder()
                .title("No Bids Lot")
                .description("No bids placed")
                .startPrice(new BigDecimal("500.00"))
                .currentPrice(new BigDecimal("500.00"))
                .bidStep(new BigDecimal("25.00"))
                .ownerId(testOwnerId)
                .status(LotStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .build();

        expiredLot = lotRepository.save(expiredLot);

        when(bidClient.getAuctionWinner(expiredLot.getId())).thenReturn(null);

        lotScheduler.closeExpiredLots();

        Lot closed = lotRepository.findById(expiredLot.getId()).orElseThrow();
        assertEquals(LotStatus.COMPLETED, closed.getStatus());
        assertNull(closed.getWinnerId());
    }

    @Test
    void closeExpiredLots_withBidServiceError_shouldCloseAnyway() {
        Lot expiredLot = Lot.builder()
                .title("Error Lot")
                .description("Bid service error")
                .startPrice(new BigDecimal("500.00"))
                .currentPrice(new BigDecimal("500.00"))
                .bidStep(new BigDecimal("25.00"))
                .ownerId(testOwnerId)
                .status(LotStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .build();

        expiredLot = lotRepository.save(expiredLot);

        when(bidClient.getAuctionWinner(expiredLot.getId()))
                .thenThrow(new RuntimeException("Bid service down"));

        lotScheduler.closeExpiredLots();

        Lot closed = lotRepository.findById(expiredLot.getId()).orElseThrow();
        assertEquals(LotStatus.COMPLETED, closed.getStatus());
        assertNull(closed.getWinnerId());
    }

    @Test
    void lotRepository_findByStatus_shouldWork() {
        Lot activeLot = Lot.builder()
                .title("Active Lot")
                .description("Active auction")
                .startPrice(new BigDecimal("300.00"))
                .currentPrice(new BigDecimal("300.00"))
                .bidStep(new BigDecimal("15.00"))
                .ownerId(testOwnerId)
                .status(LotStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .build();

        lotRepository.save(activeLot);

        var page = lotRepository.findByStatus(LotStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Active Lot", page.getContent().get(0).getTitle());
    }

    @Test
    void lotRepository_saveAndFind_shouldWork() {
        Lot newLot = Lot.builder()
                .title("Saved Lot")
                .description("Test save")
                .startPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .bidStep(new BigDecimal("5.00"))
                .ownerId(testOwnerId)
                .status(LotStatus.DRAFT)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        Lot saved = lotRepository.save(newLot);
        Optional<Lot> found = lotRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Saved Lot", found.get().getTitle());
        assertEquals(testOwnerId, found.get().getOwnerId());
    }

    @Test
    void completeLotFlow_shouldWork() {
        CreateLotDto createRequest = new CreateLotDto(
                "Complete Flow Laptop",
                "Test complete flow",
                new BigDecimal("1200.00"),
                new BigDecimal("60.00"),
                3L,
                testOwnerId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        LotDto created = lotService.createLot(createRequest, testOwnerId);
        assertNotNull(created);
        assertEquals(LotStatus.PENDING_APPROVAL, created.status());

        Lot lotInDb = lotRepository.findById(created.id()).orElseThrow();
        lotInDb = lotInDb.toBuilder()
                .status(LotStatus.PENDING_APPROVAL)
                .build();
        lotRepository.save(lotInDb);

        LotDto approved = lotService.approveLot(created.id(), testOwnerId);
        assertEquals(LotStatus.ACTIVE, approved.status());

        UpdateLotDto updateRequest = new UpdateLotDto(
                "Updated in Flow",
                "Updated description",
                new BigDecimal("75.00"),
                4L,
                LocalDateTime.now().plusDays(14)
        );

        lotInDb = lotInDb.toBuilder()
                .status(LotStatus.DRAFT)
                .build();
        lotRepository.save(lotInDb);

        LotDto updated = lotService.updateLot(created.id(), updateRequest, testOwnerId);
        assertEquals("Updated in Flow", updated.title());
        assertEquals(new BigDecimal("75.00"), updated.bid_step());

        lotService.deleteLot(created.id(), testOwnerId);

        Optional<Lot> deleted = lotRepository.findById(created.id());
        assertTrue(deleted.isEmpty());
    }

    @Test
    void auctionCompletionFlow_shouldWork() {
        Lot auctionLot = Lot.builder()
                .title("Auction Item")
                .description("Will be completed by scheduler")
                .startPrice(new BigDecimal("800.00"))
                .currentPrice(new BigDecimal("800.00"))
                .bidStep(new BigDecimal("40.00"))
                .ownerId(testOwnerId)
                .status(LotStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(3))
                .endDate(LocalDateTime.now().minusHours(1)) // Прошла
                .createdAt(LocalDateTime.now())
                .build();

        auctionLot = lotRepository.save(auctionLot);

        when(bidClient.getAuctionWinner(auctionLot.getId())).thenReturn(3L);
        when(userClient.getUserById(3L)).thenReturn(new UserDto(3L, "winner@test.com", "winner_user"));

        lotScheduler.closeExpiredLots();

        Lot completed = lotRepository.findById(auctionLot.getId()).orElseThrow();
        assertEquals(LotStatus.COMPLETED, completed.getStatus());
        assertEquals(3L, completed.getWinnerId());

        LotDto dto = lotService.getLotById(auctionLot.getId());
        assertEquals(LotStatus.COMPLETED, dto.status());
        assertEquals("winner_user", dto.winner_username());
    }


    @Test
    void lotDto_creation_shouldWork() {
        LotDto dto = new LotDto(
                1L,
                "Test Lot",
                "Description",
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                testOwnerId,
                "testuser",
                1L,
                "Electronics",
                LotStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                2L,
                "winner"
        );

        assertEquals(1L, dto.id());
        assertEquals("Test Lot", dto.title());
        assertEquals(LotStatus.ACTIVE, dto.status());
        assertEquals("testuser", dto.owner_username());
        assertEquals("winner", dto.winner_username());
    }

    @Test
    void lotStatus_enum_shouldHaveAllValues() {
        assertEquals(LotStatus.ACTIVE, LotStatus.valueOf("ACTIVE"));
        assertEquals(LotStatus.COMPLETED, LotStatus.valueOf("COMPLETED"));
        assertEquals(LotStatus.CANCELLED, LotStatus.valueOf("CANCELLED"));
        assertEquals(LotStatus.PENDING_APPROVAL, LotStatus.valueOf("PENDING_APPROVAL"));
        assertEquals(LotStatus.DRAFT, LotStatus.valueOf("DRAFT"));
    }
}