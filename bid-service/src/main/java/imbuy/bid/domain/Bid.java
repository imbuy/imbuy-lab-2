package imbuy.bid.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("bids")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid {
    @Id
    private Long id;

    @Column("lot_id")
    private Long lotId;

    @Column("bidder_id")
    private Long bidderId;

    @Column("amount")
    private BigDecimal amount;

    @Column("created_at")
    private LocalDateTime createdAt;
}