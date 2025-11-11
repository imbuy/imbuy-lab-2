package imbuy.lot.domain;

import imbuy.lot.enums.LotStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lots")
public class Lot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Start price must be greater than 0")
    @Column(name = "start_price", precision = 19, scale = 2)
    private BigDecimal startPrice;

    @NotNull
    @Column(name = "current_price", precision = 19, scale = 2)
    private BigDecimal currentPrice;

    @NotNull(message = "Bid step is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Bid step must be greater than 0")
    @Column(name = "bid_step", precision = 19, scale = 2)
    private BigDecimal bidStep;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "winner_id")
    private Long winnerId;

    @Enumerated(EnumType.STRING)
    @NotNull
    private LotStatus status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Конструкторы
    public Lot() {}

    public Lot(Long id, String title, String description, BigDecimal startPrice,
               BigDecimal currentPrice, BigDecimal bidStep, Long ownerId, Long categoryId,
               Long winnerId, LotStatus status, LocalDateTime startDate,
               LocalDateTime endDate, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.bidStep = bidStep;
        this.ownerId = ownerId;
        this.categoryId = categoryId;
        this.winnerId = winnerId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getStartPrice() { return startPrice; }
    public void setStartPrice(BigDecimal startPrice) { this.startPrice = startPrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getBidStep() { return bidStep; }
    public void setBidStep(BigDecimal bidStep) { this.bidStep = bidStep; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }

    public LotStatus getStatus() { return status; }
    public void setStatus(LotStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static LotBuilder builder() {
        return new LotBuilder();
    }

    public static class LotBuilder {
        private Long id;
        private String title;
        private String description;
        private BigDecimal startPrice;
        private BigDecimal currentPrice;
        private BigDecimal bidStep;
        private Long ownerId;
        private Long categoryId;
        private Long winnerId;
        private LotStatus status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime createdAt;

        public LotBuilder id(Long id) { this.id = id; return this; }
        public LotBuilder title(String title) { this.title = title; return this; }
        public LotBuilder description(String description) { this.description = description; return this; }
        public LotBuilder startPrice(BigDecimal startPrice) { this.startPrice = startPrice; return this; }
        public LotBuilder currentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; return this; }
        public LotBuilder bidStep(BigDecimal bidStep) { this.bidStep = bidStep; return this; }
        public LotBuilder ownerId(Long ownerId) { this.ownerId = ownerId; return this; }
        public LotBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public LotBuilder winnerId(Long winnerId) { this.winnerId = winnerId; return this; }
        public LotBuilder status(LotStatus status) { this.status = status; return this; }
        public LotBuilder startDate(LocalDateTime startDate) { this.startDate = startDate; return this; }
        public LotBuilder endDate(LocalDateTime endDate) { this.endDate = endDate; return this; }
        public LotBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Lot build() {
            return new Lot(id, title, description, startPrice, currentPrice, bidStep,
                    ownerId, categoryId, winnerId, status, startDate, endDate, createdAt);
        }
    }
}