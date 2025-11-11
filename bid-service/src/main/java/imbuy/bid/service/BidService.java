package imbuy.bid.service;

import imbuy.bid.domain.Bid;
import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.dto.PageResponse;
import imbuy.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
// исправить это все
public class BidService {

    private final BidRepository bidRepository;

    public PageResponse<BidDto> getBidsByLotId(Long lotId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByLotIdOrderByCreatedAtDesc(lotId, pageable);
        return PageResponse.of(bids.map(this::mapToDto));
    }

    public BidDto placeBid(Long lotId, CreateBidDto createBidDto, Long currentUserId) {
        // Временная валидация - надо вызов Lot Service
        validateBid(lotId, createBidDto.amount(), currentUserId);

        Bid bid = Bid.builder()
                .lotId(lotId)
                .bidderId(currentUserId)
                .amount(createBidDto.amount())
                .createdAt(LocalDateTime.now())
                .build();

        Bid savedBid = bidRepository.save(bid);
        return mapToDto(savedBid);
    }

    public BidDto getWinningBid(Long lotId) {
        return bidRepository.findTopByLotIdOrderByAmountDesc(lotId)
                .map(this::mapToDto)
                .orElse(null);
    }

    public int countBidsByLot(Long lotId) {
        return Math.toIntExact(bidRepository.countByLotId(lotId));
    }

    public Optional<Bid> getHighestBidByLot(Long lotId) {
        return bidRepository.findTopByLotIdOrderByAmountDesc(lotId);
    }

    private void validateBid(Long lotId, BigDecimal amount, Long bidderId) {
        // Временная валидация - в реальном приложении здесь был бы вызов Lot Service
        // для проверки статуса лота, текущей цены и т.д.

        // Проверяем, есть ли уже ставки на этот лот
        Optional<BigDecimal> maxBid = bidRepository.findMaxBidAmountByLotId(lotId);

        if (maxBid.isPresent()) {
            BigDecimal minBid = maxBid.get().add(new BigDecimal("10.00")); // временный шаг
            if (amount.compareTo(minBid) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Bid must be at least %.2f", minBid));
            }
        }

        // Проверяем, что пользователь не делает ставку на свой же лот
        // В реальном приложении здесь был бы вызов Lot Service для получения ownerId
        Long lotOwnerId = 1L; // временное значение
        if (bidderId.equals(lotOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Owner cannot place a bid on their own lot");
        }
    }

    private BidDto mapToDto(Bid bid) {
        return new BidDto(
                bid.getId(),
                bid.getAmount(),
                bid.getBidderId(),
                "User " + bid.getBidderId() // временное значение
        );
    }
}
