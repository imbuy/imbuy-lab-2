package imbuy.bid.repository;

import imbuy.bid.domain.Bid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public interface BidRepository extends ReactiveCrudRepository<Bid, Long> {

    Flux<Bid> findByLotIdOrderByCreatedAtDesc(Long lotId, Pageable pageable);

    @Query("""
                SELECT * FROM bids 
                WHERE lot_id = :lotId 
                ORDER BY amount DESC, created_at DESC 
                LIMIT 1
            """)
    Mono<Bid> findHighestBidByLotId(Long lotId);

    Mono<Long> countByLotId(Long lotId);

    @Query("SELECT MAX(amount) FROM bids WHERE lot_id = $1")
    Mono<BigDecimal> findMaxBidAmountByLotId(Long lotId);

    @Query("SELECT COUNT(*) FROM bids WHERE lot_id = :lotId")
    Mono<Long> countBidsForLot(Long lotId);
}
