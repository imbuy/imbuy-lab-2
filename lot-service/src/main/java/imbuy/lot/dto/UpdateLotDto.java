package imbuy.lot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateLotDto(
        String title,
        String description,
        BigDecimal bid_step,
        Long category_id,
        LocalDateTime end_date
) {}