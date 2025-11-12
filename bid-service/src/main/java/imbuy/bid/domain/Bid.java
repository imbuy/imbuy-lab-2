package imbuy.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("bids")
public class Bid {
    @Id
    private Long id;
    private Long lotId;
    private Long bidderId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}