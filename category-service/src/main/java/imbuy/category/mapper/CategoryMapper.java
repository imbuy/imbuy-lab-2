package imbuy.category.mapper;

import imbuy.category.domain.Category;
import imbuy.category.dto.CategoryRequest;
import imbuy.category.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "parentId", source = "parentId")
    Category toEntity(CategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "parentId", source = "parentId")
    Category updateEntity(CategoryRequest request, @MappingTarget Category category);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", source = "category.id")
    @Mapping(target = "name", source = "category.name")
    @Mapping(target = "parentId", source = "category.parentId")
    @Mapping(target = "children", source = "children")
    CategoryResponse toResponseWithChildren(Category category, List<CategoryResponse> children);
}