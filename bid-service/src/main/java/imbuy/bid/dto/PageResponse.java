package imbuy.bid.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int current_page,
        int page_size,
        boolean has_next,
        boolean has_previous
) {}
