package imbuy.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CategoryTreeDto(
        List<CategoryNode> categories
) {
    public record CategoryNode(
            Long id,
            String name,
            Long parentId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("children")
            List<CategoryNode> children
    ) {}
}