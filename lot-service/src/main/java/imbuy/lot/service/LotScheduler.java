package imbuy.lot.service;

import imbuy.lot.client.BidClient;
import imbuy.lot.domain.Lot;
import imbuy.lot.enums.LotStatus;
import imbuy.lot.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotScheduler {

    private final LotRepository lotRepository;
    private final BidClient bidClient;

    @Scheduled(fixedRate = 60000)
    public void closeExpiredLots() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Lot scheduler started at: {}", now);

        int page = 0;
        int size = 100;
        int closedCount = 0;

        Page<Lot> activeLots;

        do {
            activeLots = lotRepository.findByStatus(LotStatus.ACTIVE, PageRequest.of(page, size));
            log.info("Checking page {} ({} active lots)", page, activeLots.getNumberOfElements());

            for (Lot lot : activeLots.getContent()) {
                if (lot.getEndDate() != null && lot.getEndDate().isBefore(now)) {
                    log.info("Closing expired lot #{} ('{}')", lot.getId(), lot.getTitle());
                    closeLotWithWinner(lot);
                    closedCount++;
                }
            }

            page++;
        } while (activeLots.hasNext());

        log.info("Lot scheduler finished. Closed {} lots.", closedCount);
    }

    @Transactional
    public void closeLotWithWinner(Lot lot) {
        try {
            log.info("Requesting winner for lot #{} from bid-service", lot.getId());
            Long winnerId = bidClient.getAuctionWinner(lot.getId());
            log.info("Received winner ID for lot #{}: {}", lot.getId(), winnerId);

            Lot updatedLot = lot.toBuilder()
                    .status(LotStatus.COMPLETED)
                    .winnerId(winnerId)
                    .build();

            lotRepository.save(updatedLot);

            if (winnerId != null) {
                log.info("Lot #{} completed. Winner: user #{}", lot.getId(), winnerId);
            } else {
                log.warn("Lot #{} completed without winner (no bids or error)", lot.getId());
            }

        } catch (Exception e) {
            log.error("Error closing lot #{}: {}", lot.getId(), e.getMessage(), e);
            Lot updatedLot = lot.toBuilder()
                    .status(LotStatus.COMPLETED)
                    .winnerId(null)
                    .build();
            lotRepository.save(updatedLot);
        }
    }
}