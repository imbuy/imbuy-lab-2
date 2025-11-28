package imbuy.lot.service;

import imbuy.lot.client.UserClient;
import imbuy.lot.domain.Lot;
import imbuy.lot.dto.*;
import imbuy.lot.enums.LotStatus;
import imbuy.lot.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LotService {

    private final UserClient userClient;
    private final LotRepository lotRepository;

    public PageResponse<LotDto> getLots(LotFilterDto filter, Pageable pageable, Long currentUserId) {
        Page<Lot> lots;

        if (filter != null && hasFilters(filter)) {
            lots = lotRepository.findByFilters(
                    filter.title(),
                    filter.status(),
                    filter.category_id(),
                    filter.owner_id(),
                    pageable
            );
        } else if (filter != null && Boolean.TRUE.equals(filter.active_only())) {
            lots = lotRepository.findByStatus(LotStatus.ACTIVE, pageable);
        } else {
            lots = lotRepository.findAll(pageable);
        }

        return PageResponse.of(lots.map(this::mapToDto));
    }

    public LotDto getLotById(Long id) {
        Lot lot = getLotEntityById(id);
        return mapToDto(lot);
    }

    public Lot getLotEntityById(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot not found"));
    }

    public LotDto createLot(CreateLotDto createLotDto, Long ownerId) {
        Lot lot = Lot.builder()
                .title(createLotDto.title())
                .description(createLotDto.description())
                .startPrice(createLotDto.start_price())
                .currentPrice(createLotDto.start_price())
                .bidStep(createLotDto.bid_step())
                .ownerId(ownerId)
                .categoryId(createLotDto.category_id())
                .status(LotStatus.PENDING_APPROVAL)
                .startDate(createLotDto.start_date() != null ? createLotDto.start_date() : LocalDateTime.now())
                .endDate(createLotDto.end_date())
                .createdAt(LocalDateTime.now())
                .build();

        Lot savedLot = lotRepository.save(lot);
        return mapToDto(savedLot);
    }

    public LotDto approveLot(Long lotId, Long currentUserId) {
        Lot lot = getLotEntityById(lotId);
        checkOwnership(lot, currentUserId);

        if (lot.getStatus() != LotStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lot is not awaiting approval");
        }

        Lot updatedLot = lot.toBuilder()
                .status(LotStatus.ACTIVE)
                .build();
        lotRepository.save(updatedLot);

        return mapToDto(updatedLot);
    }

    public LotDto cancelLot(Long lotId, Long currentUserId, String reason) {
        Lot lot = getLotEntityById(lotId);
        checkOwnership(lot, currentUserId);

        if (lot.getStatus() != LotStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lot cannot be rejected");
        }

        Lot updatedLot = lot.toBuilder()
                .status(LotStatus.CANCELLED)
                .build();
        lotRepository.save(updatedLot);

        return mapToDto(updatedLot);
    }

    public LotDto updateLot(Long id, UpdateLotDto updateLotDto, Long currentUserId) {
        Lot lot = getLotEntityById(id);
        checkOwnership(lot, currentUserId);

        if (lot.getStatus() != LotStatus.DRAFT && lot.getStatus() != LotStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update lot in current status");
        }
        Lot.LotBuilder lotBuilder = lot.toBuilder();
        if (updateLotDto.title() != null) lotBuilder.title(updateLotDto.title());
        if (updateLotDto.description() != null) lotBuilder.description(updateLotDto.description());
        if (updateLotDto.bid_step() != null) lotBuilder.bidStep(updateLotDto.bid_step());
        if (updateLotDto.end_date() != null) lotBuilder.endDate(updateLotDto.end_date());
        if (updateLotDto.category_id() != null) lotBuilder.categoryId(updateLotDto.category_id());

        Lot updatedLot = lotRepository.save(lot);
        return mapToDto(updatedLot);
    }

    public void deleteLot(Long id, Long currentUserId) {
        Lot lot = getLotEntityById(id);
        checkOwnership(lot, currentUserId);

        if (lot.getStatus() == LotStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete active lot");
        }

        lotRepository.delete(lot);
    }

    private LotDto mapToDto(Lot lot) {
        UserDto owner = userClient.getUserById(lot.getOwnerId());
        String ownerName = owner != null ? owner.username() : "Unknown";

        return new LotDto(
                lot.getId(),
                lot.getTitle(),
                lot.getDescription(),
                lot.getStartPrice(),
                lot.getCurrentPrice(),
                lot.getBidStep(),
                lot.getOwnerId(),
                ownerName,
                lot.getCategoryId(),
                lot.getCategoryId() != null ? "Category " + lot.getCategoryId() : null,
                lot.getStatus(),
                lot.getStartDate(),
                lot.getEndDate(),
                lot.getWinnerId(),
                lot.getWinnerId() != null ? "Winner " + lot.getWinnerId() : null
        );
    }

    private boolean hasFilters(LotFilterDto filter) {
        return filter.title() != null || filter.status() != null ||
                filter.category_id() != null || filter.owner_id() != null;
    }

    private void checkOwnership(Lot lot, Long userId) {
        if (!lot.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own lots");
        }
    }
}