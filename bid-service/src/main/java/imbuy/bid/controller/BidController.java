package imbuy.bid.controller;

import imbuy.bid.dto.BidDto;
import imbuy.bid.dto.CreateBidDto;
import imbuy.bid.dto.PageResponse;
import imbuy.bid.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Bid management APIs")
public class BidController {

    private final BidService bidService;

    @GetMapping("/lots/{lotId}")
    @Operation(summary = "Get bid history for a lot")
    public ResponseEntity<PageResponse<BidDto>> getBidsByLotId(
            @PathVariable Long lotId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        PageResponse<BidDto> bids = bidService.getBidsByLotId(lotId, pageable);
        return ResponseEntity.ok(bids);
    }

    @PostMapping("/lots/{lotId}")
    @Operation(summary = "Place a bid on a lot")
    public ResponseEntity<BidDto> placeBid(
            @PathVariable Long lotId,
            @RequestParam Long currentUserId,
            @Valid @RequestBody CreateBidDto createBidDto) {

        BidDto bid = bidService.placeBid(lotId, createBidDto, currentUserId);
        return new ResponseEntity<>(bid, HttpStatus.CREATED);
    }

    @GetMapping("/lots/{lotId}/winning")
    @Operation(summary = "Get winning bid for a lot")
    public ResponseEntity<BidDto> getWinningBid(@PathVariable Long lotId) {
        BidDto winningBid = bidService.getWinningBid(lotId);
        return winningBid != null ? ResponseEntity.ok(winningBid) : ResponseEntity.notFound().build();
    }
}
