package imbuy.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CategoryResponse(
        Long id,

        @NotBlank(message = "Category name is required")
        String name,

        Long parentId,

        @JsonProperty(value = "children", access = JsonProperty.Access.READ_ONLY)
        List<CategoryResponse> children
) {}