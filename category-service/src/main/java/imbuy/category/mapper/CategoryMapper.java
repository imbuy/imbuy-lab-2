package imbuy.category.mapper;

import imbuy.category.domain.Category;
import imbuy.category.dto.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "parent_id", source = "parentId")
    @Mapping(target = "children", ignore = true)
    CategoryDto toDto(Category category);

    @Mapping(target = "id", source = "category.id")
    @Mapping(target = "name", source = "category.name")
    @Mapping(target = "parent_id", source = "category.parentId")
    @Mapping(target = "children", source = "children")
    CategoryDto toDtoWithChildren(Category category, String parentName, List<CategoryDto> children);

}