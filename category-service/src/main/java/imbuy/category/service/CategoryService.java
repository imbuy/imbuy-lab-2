package imbuy.category.service;

import imbuy.category.domain.Category;
import imbuy.category.dto.CategoryDto;
import imbuy.category.dto.CategoryTreeDto;
import imbuy.category.dto.PageResponse;
import imbuy.category.mapper.CategoryMapper;
import imbuy.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryTreeDto getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildren();
        return new CategoryTreeDto(
                rootCategories.stream()
                        .map(categoryMapper::toDtoWithChildren)
                        .toList()
        );
    }

    public PageResponse<CategoryDto> getAllCategories(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        return PageResponse.of(categories.map(categoryMapper::toDtoWithChildren));
    }

    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return categoryMapper.toDtoWithChildren(category);
    }

    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.existsByNameAndParentId(categoryDto.name(), categoryDto.parent_id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category with this name already exists");
        }

        Category category = new Category();
        category.setName(categoryDto.name());

        if (categoryDto.parent_id() != null) {
            Category parent = categoryRepository.findById(categoryDto.parent_id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent category not found"));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        return categoryMapper.toDtoWithChildren(category);
    }

    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        category.setName(categoryDto.name());

        if (categoryDto.parent_id() != null) {
            Category parent = categoryRepository.findById(categoryDto.parent_id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        return categoryMapper.toDtoWithChildren(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (!category.getChildren().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete category with subcategories");
        }

        // Убрана проверка на lots, т.к. она в другом сервисе
        categoryRepository.delete(category);
    }
}