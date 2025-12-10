package imbuy.category.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        boolean hasNext,
        boolean hasPrevious
) {}