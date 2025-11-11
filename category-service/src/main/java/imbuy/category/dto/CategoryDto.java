package imbuy.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CategoryDto(
        Long id,
        @NotBlank(message = "Category name is required")
        String name,
        Long parent_id,
        String parent_name,
        @JsonProperty(value = "children", access = JsonProperty.Access.READ_ONLY)
        List<CategoryDto> children
) {}