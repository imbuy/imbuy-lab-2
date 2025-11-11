package imbuy.bid.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateBidDto(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
        BigDecimal amount
) {}
