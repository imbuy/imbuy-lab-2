package imbuy.category.mapper;

import imbuy.category.domain.Category;
import imbuy.category.dto.CategoryDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getParentId(),
                null,
                null
        );
    }

    public CategoryDto toDtoWithChildren(Category category, String parentName, List<CategoryDto> children) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getParentId(),
                parentName,
                children
        );
    }
}