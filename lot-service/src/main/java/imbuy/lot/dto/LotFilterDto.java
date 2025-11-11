package imbuy.lot.dto;

import imbuy.lot.enums.LotStatus;

public record LotFilterDto(
        String title,
        LotStatus status,
        Long category_id,
        Long owner_id,
        Boolean active_only
) {}
