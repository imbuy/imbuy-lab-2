package imbuy.lot.dto;

import imbuy.lot.enums.LotStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LotDto(
        Long id,
        String title,
        String description,
        BigDecimal start_price,
        BigDecimal current_price,
        BigDecimal bid_step,
        Long owner_id,
        String owner_username,
        Long category_id,
        String category_name,
        LotStatus status,
        LocalDateTime start_date,
        LocalDateTime end_date,
        Long winner_id,
        String winner_username
) {}