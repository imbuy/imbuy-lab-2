package imbuy.category.service;

import imbuy.category.domain.Category;
import imbuy.category.dto.*;
import imbuy.category.mapper.CategoryMapper;
import imbuy.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public Mono<CategoryTreeDto> getCategoryTree() {
        return categoryRepository.findRootCategories()
                .flatMap(this::buildCategoryWithChildren)
                .collectList()
                .map(CategoryTreeDto::new);
    }

    private Mono<CategoryTreeDto.CategoryNode> buildCategoryWithChildren(Category category) {
        Mono<List<CategoryTreeDto.CategoryNode>> childrenMono = categoryRepository.findByParentId(category.getId())
                .flatMap(this::buildCategoryWithChildren)
                .collectList();

        return childrenMono
                .map(children -> new CategoryTreeDto.CategoryNode(
                        category.getId(),
                        category.getName(),
                        category.getParentId(),
                        children.isEmpty() ? null : children
                ));
    }

    public Flux<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll()
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(categoryMapper::toResponse);
    }

    public Mono<CategoryResponse> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> {
                    Mono<List<CategoryResponse>> childrenMono = categoryRepository.findByParentId(id)
                            .map(categoryMapper::toResponse)
                            .collectList();

                    return childrenMono
                            .map(children -> categoryMapper.toResponseWithChildren(
                                    category,
                                    children.isEmpty() ? null : children
                            ));
                });
    }

    public Mono<CategoryResponse> createCategory(CategoryRequest categoryRequest) {
        return categoryRepository.existsByNameAndParentId(categoryRequest.name(), categoryRequest.parentId())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Category with this name already exists"));
                    }

                    if (categoryRequest.parentId() != null) {
                        return categoryRepository.findById(categoryRequest.parentId())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Parent category not found")))
                                .then(createCategoryEntity(categoryRequest));
                    }

                    return createCategoryEntity(categoryRequest);
                });
    }

    private Mono<CategoryResponse> createCategoryEntity(CategoryRequest categoryRequest) {
        Category category = categoryMapper.toEntity(categoryRequest);

        return categoryRepository.save(category)
                .map(categoryMapper::toResponse);
    }

    public Mono<CategoryResponse> updateCategory(Long id, CategoryRequest categoryRequest) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> {
                    if (categoryRequest.parentId() != null) {
                        return categoryRepository.findById(categoryRequest.parentId())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Parent category not found")))
                                .then(updateCategoryEntity(id, categoryRequest, category));
                    }
                    return updateCategoryEntity(id, categoryRequest, category);
                });
    }

    private Mono<CategoryResponse> updateCategoryEntity(Long id, CategoryRequest categoryRequest, Category existingCategory) {
        categoryMapper.updateEntity(categoryRequest, existingCategory);

        return categoryRepository.save(existingCategory)
                .map(categoryMapper::toResponse);
    }

    public Mono<Void> deleteCategory(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> categoryRepository.findByParentId(id)
                        .hasElements()
                        .flatMap(hasChildren -> {
                            if (Boolean.TRUE.equals(hasChildren)) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Cannot delete category with subcategories"));
                            }
                            return categoryRepository.deleteById(id);
                        }));
    }
}