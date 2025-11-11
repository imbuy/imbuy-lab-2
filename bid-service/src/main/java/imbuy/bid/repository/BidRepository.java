package imbuy.bid.repository;

import imbuy.bid.domain.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    Page<Bid> findByLotIdOrderByCreatedAtDesc(Long lotId, Pageable pageable);

    Optional<Bid> findTopByLotIdOrderByAmountDesc(Long lotId);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.lotId = :lotId")
    Long countByLotId(@Param("lotId") Long lotId);

    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.lotId = :lotId")
    Optional<BigDecimal> findMaxBidAmountByLotId(@Param("lotId") Long lotId);
}
