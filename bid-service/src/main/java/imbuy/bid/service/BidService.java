package imbuy.bid.service;

import imbuy.bid.domain.Bid;
import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.dto.PageResponse;
import imbuy.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;

    public Mono<PageResponse<BidDto>> getBidsByLotId(Long lotId, Pageable pageable) {
        return bidRepository.findByLotIdOrderByCreatedAtDesc(lotId, pageable)
                .map(this::mapToDto)
                .collectList()
                .flatMap(bids -> {
                    return bidRepository.countByLotId(lotId)
                            .map(total -> {
                                int pageNumber = pageable.getPageNumber();
                                int pageSize = pageable.getPageSize();
                                boolean hasNext = (pageNumber + 1) * pageSize < total;
                                boolean hasPrevious = pageNumber > 0;

                                return new PageResponse<>(
                                        bids,
                                        pageNumber,
                                        pageSize,
                                        hasNext,
                                        hasPrevious
                                );
                            });
                });
    }

    public Mono<BidDto> placeBid(Long lotId, CreateBidDto createBidDto, Long currentUserId) {
        return validateBid(lotId, createBidDto.amount(), currentUserId)
                .then(createBid(lotId, createBidDto.amount(), currentUserId))
                .flatMap(bidRepository::save)
                .map(this::mapToDto);
    }

    public Mono<BidDto> getWinningBid(Long lotId) {
        return bidRepository.findHighestBidByLotId(lotId)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Integer> countBidsByLot(Long lotId) {
        return bidRepository.countByLotId(lotId)
                .map(Math::toIntExact);
    }

    public Mono<Bid> getHighestBidByLot(Long lotId) {
        return bidRepository.findHighestBidByLotId(lotId);
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

    private BidDto mapToDto(Bid bid) {
        return new BidDto(
                bid.getId(),
                bid.getAmount(),
                bid.getBidderId(),
                "User " + bid.getBidderId()
        );
    }

    public Mono<Long> getAuctionWinnerId(Long lotId) {
        return bidRepository.findHighestBidByLotId(lotId)
                .map(Bid::getBidderId)
                .switchIfEmpty(Mono.empty())
                .doOnNext(winnerId -> {
                    if (winnerId != null) {
                        System.out.println("Found winner for lot " + lotId + ": " + winnerId);
                    } else {
                        System.out.println("No winner found for lot " + lotId);
                    }
                })
                .onErrorReturn(null);
    }
}
