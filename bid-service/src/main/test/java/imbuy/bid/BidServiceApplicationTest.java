package imbuy.bid;

import imbuy.bid.domain.Bid;
import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.mapper.BidMapper;
import imbuy.bid.repository.BidRepository;
import imbuy.bid.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class BidServiceApplicationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bid_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BidMapper bidMapper;

    private final Long testLotId = 1L;
    private Bid testBid;

    @BeforeEach
    void beforeEach() {
        bidRepository.deleteAll().block();

        Long testBidderId = 1L;
        testBid = Bid.builder()
                .lotId(testLotId)
                .bidderId(testBidderId)
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();

        testBid = bidRepository.save(testBid).block();
    }

    @Test
    void placeBid_shouldCreateNewBid() {
        CreateBidDto request = new CreateBidDto(new BigDecimal("110.00"));

        BidDto result = bidService.placeBid(testLotId, request, 2L).block();

        assertNotNull(result);
        assertEquals(new BigDecimal("110.00"), result.amount());
        assertEquals(2L, result.bidder_id());
        assertEquals("User 2", result.bidder_username());
    }

    @Test
    void placeBid_shouldValidateMinimumBid() {
        CreateBidDto request = new CreateBidDto(new BigDecimal("105.00"));

        StepVerifier.create(bidService.placeBid(testLotId, request, 2L))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Bid must be at least"))
                .verify();
    }

    @Test
    void getBidsByLotId_shouldReturnPaginated() {
        Bid anotherBid = Bid.builder()
                .lotId(testLotId)
                .bidderId(2L)
                .amount(new BigDecimal("150.00"))
                .createdAt(LocalDateTime.now().plusMinutes(1))
                .build();
        bidRepository.save(anotherBid).block();

        List<BidDto> result = bidService.getBidsByLotId(testLotId,
                        PageRequest.of(0, 10))
                .collectList()
                .block();

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(new BigDecimal("150.00"), result.get(0).amount());
        assertEquals(new BigDecimal("100.00"), result.get(1).amount());
    }

    @Test
    void getBidsByLotId_shouldReturnCorrectPageSize() {
        for (int i = 0; i < 15; i++) {
            Bid bid = Bid.builder()
                    .lotId(testLotId)
                    .bidderId((long) i)
                    .amount(new BigDecimal(200 + i * 10 + ".00"))
                    .createdAt(LocalDateTime.now().plusMinutes(i))
                    .build();
            bidRepository.save(bid).block();
        }

        List<BidDto> page1 = bidService.getBidsByLotId(testLotId,
                        PageRequest.of(0, 10))
                .collectList()
                .block();

        List<BidDto> page2 = bidService.getBidsByLotId(testLotId,
                        PageRequest.of(1, 10))
                .collectList()
                .block();

        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(10, page1.size());
        assertEquals(6, page2.size());
    }

    @Test
    void getAuctionWinnerId_shouldReturnHighestBidder() {
        Bid higherBid = Bid.builder()
                .lotId(testLotId)
                .bidderId(2L)
                .amount(new BigDecimal("200.00"))
                .createdAt(LocalDateTime.now().plusMinutes(1))
                .build();
        bidRepository.save(higherBid).block();

        Long winnerId = bidService.getAuctionWinnerId(testLotId).block();

        assertEquals(2L, winnerId);
    }

    @Test
    void getAuctionWinnerId_shouldReturnZeroWhenNoBids() {
        Long emptyLotId = 999L;

        Long winnerId = bidService.getAuctionWinnerId(emptyLotId).block();

        assertEquals(0L, winnerId);
    }

    @Test
    void bidRepository_countByLotId_shouldWork() {
        Long count = bidRepository.countByLotId(testLotId).block();

        assertEquals(1L, count);
    }

    @Test
    void bidRepository_findMaxBidAmountByLotId_shouldWork() {
        Bid higherBid = Bid.builder()
                .lotId(testLotId)
                .bidderId(2L)
                .amount(new BigDecimal("200.00"))
                .build();
        bidRepository.save(higherBid).block();

        BigDecimal maxAmount = bidRepository.findMaxBidAmountByLotId(testLotId).block();

        assertNotNull(maxAmount);
        assertEquals(new BigDecimal("200.00"), maxAmount);
    }

    @Test
    void bidMapper_mapToDto_shouldMapCorrectly() {
        BidDto dto = bidMapper.mapToDto(testBid);

        assertNotNull(dto);
        assertEquals(testBid.getId(), dto.id());
        assertEquals(testBid.getAmount(), dto.amount());
        assertEquals(testBid.getBidderId(), dto.bidder_id());
        assertEquals("User " + testBid.getBidderId(), dto.bidder_username());
    }

    @Test
    void completeBidFlow_shouldWork() {
        bidService.placeBid(10L,
                new CreateBidDto(new BigDecimal("100.00")), 1L).block();
        BidDto bid2 = bidService.placeBid(10L,
                new CreateBidDto(new BigDecimal("150.00")), 2L).block();

        Long count = bidRepository.countByLotId(10L).block();
        assertEquals(2L, count);

        Long winnerId = bidService.getAuctionWinnerId(10L).block();
        assertEquals(2L, winnerId);

        Bid highestBid = bidRepository.findHighestBidByLotId(10L).block();
        BidDto mappedDto = bidMapper.mapToDto(highestBid);
        assertNotNull(bid2);
        assertEquals(bid2.id(), mappedDto.id());

        List<BidDto> page = bidService.getBidsByLotId(10L,
                        PageRequest.of(0, 10))
                .collectList()
                .block();
        assertNotNull(page);
        assertEquals(2, page.size());
        assertEquals(bid2.amount(), page.get(0).amount());
    }

    @Test
    void concurrentBids_shouldNotBreak() throws InterruptedException {
        int threadCount = 3;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int userId = i + 1;
            final BigDecimal amount = new BigDecimal(100 + userId * 20 + ".00");
            threads[i] = new Thread(() -> {
                bidService.placeBid(20L,
                        new CreateBidDto(amount),
                        (long) userId).block();
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Long count = bidRepository.countByLotId(20L).block();
        assertEquals(threadCount, count);

        Bid highest = bidRepository.findHighestBidByLotId(20L).block();
        assertNotNull(highest);
        assertEquals(new BigDecimal("160.00"), highest.getAmount());
    }

    @Test
    void firstBidOnLot_shouldNotRequireValidation() {
        Long newLotId = 40L;
        CreateBidDto firstBid = new CreateBidDto(new BigDecimal("50.00"));

        BidDto result = bidService.placeBid(newLotId, firstBid, 1L).block();

        assertNotNull(result);
        assertEquals(new BigDecimal("50.00"), result.amount());
    }

    @Test
    void getBidsByLotId_withLargePageSize_shouldRespectMaxSize() {
        for (int i = 0; i < 30; i++) {
            Bid bid = Bid.builder()
                    .lotId(testLotId)
                    .bidderId((long) i)
                    .amount(new BigDecimal(300 + i * 10 + ".00"))
                    .createdAt(LocalDateTime.now().plusMinutes(i))
                    .build();
            bidRepository.save(bid).block();
        }

        List<BidDto> result = bidService.getBidsByLotId(testLotId,
                        PageRequest.of(0, 100))
                .collectList()
                .block();

        assertNotNull(result);
        assertEquals(31, result.size());
    }
}