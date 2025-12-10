package imbuy.bid.service;

import imbuy.bid.domain.Bid;
import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.mapper.BidMapper;
import imbuy.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final BidMapper bidMapper;

    public Flux<BidDto> getBidsByLotId(Long lotId, Pageable pageable) {
        return bidRepository.findByLotIdOrderByCreatedAtDesc(lotId, pageable)
                .map(bidMapper::mapToDto);
    }

    public Mono<BidDto> placeBid(Long lotId, CreateBidDto createBidDto, Long currentUserId) {
        return validateBid(lotId, createBidDto.amount(), currentUserId)
                .then(createBid(lotId, createBidDto.amount(), currentUserId))
                .flatMap(bidRepository::save)
                .map(bidMapper::mapToDto);
    }

    private Mono<Void> validateBid(Long lotId, BigDecimal amount, Long bidderId) {
        return bidRepository.findMaxBidAmountByLotId(lotId)
                .flatMap(maxBid -> {
                    if (maxBid != null) {
                        BigDecimal minBid = maxBid.add(new BigDecimal("10.00"));
                        if (amount.compareTo(minBid) < 0) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    String.format("Bid must be at least %.2f", minBid)));
                        }
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.empty())
                .then();
    }

    private Mono<Bid> createBid(Long lotId, BigDecimal amount, Long bidderId) {
        Bid bid = Bid.builder()
                .lotId(lotId)
                .bidderId(bidderId)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
        return Mono.just(bid);
    }

    public Mono<Long> getAuctionWinnerId(Long lotId) {
        return bidRepository.countBidsForLot(lotId)
                .doOnNext(count -> System.out.println("Total bids in DB for lot " + lotId + ": " + count))
                .flatMap(count -> {
                    if (count > 0) {
                        return bidRepository.findHighestBidByLotId(lotId)
                                .doOnNext(bid -> {
                                    System.out.println("=== FOUND BID FROM DB ===");
                                    System.out.println("Bid ID: " + bid.getId());
                                    System.out.println("Bidder ID: " + bid.getBidderId());
                                    System.out.println("Amount: " + bid.getAmount());
                                    System.out.println("Lot ID: " + bid.getLotId());
                                    System.out.println("Created At: " + bid.getCreatedAt());
                                    System.out.println("==========================");
                                })
                                .map(Bid::getBidderId)
                                .doOnNext(winnerId -> System.out.println("Mapped winner ID: " + winnerId));
                    } else {
                        return Mono.just(0L);
                    }
                })
                .doOnError(error -> {
                    System.out.println("ERROR: " + error.getMessage());
                })
                .defaultIfEmpty(0L)
                .doOnNext(finalResult -> System.out.println("FINAL RESULT: " + finalResult));
    }
}