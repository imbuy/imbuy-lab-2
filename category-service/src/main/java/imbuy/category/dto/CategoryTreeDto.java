package imbuy.category.dto;

import java.util.List;

public record CategoryTreeDto(
        List<CategoryDto> categories
) {}