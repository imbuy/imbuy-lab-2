package imbuy.bid.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Bid amount must be greater than 0")
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Конструкторы
    public Bid() {}

    public Bid(Long id, Long lotId, Long bidderId, BigDecimal amount, LocalDateTime createdAt) {
        this.id = id;
        this.lotId = lotId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }

    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static BidBuilder builder() {
        return new BidBuilder();
    }

    public static class BidBuilder {
        private Long id;
        private Long lotId;
        private Long bidderId;
        private BigDecimal amount;
        private LocalDateTime createdAt;

        public BidBuilder id(Long id) { this.id = id; return this; }
        public BidBuilder lotId(Long lotId) { this.lotId = lotId; return this; }
        public BidBuilder bidderId(Long bidderId) { this.bidderId = bidderId; return this; }
        public BidBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public BidBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Bid build() {
            return new Bid(id, lotId, bidderId, amount, createdAt);
        }
    }
}
