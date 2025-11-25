package imbuy.lot.domain;

import imbuy.lot.enums.LotStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lots")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Lot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
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
    private LotStatus status = LotStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}