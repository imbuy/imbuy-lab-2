package imbuy.category.service;

import imbuy.category.domain.Category;
import imbuy.category.dto.CategoryDto;
import imbuy.category.dto.CategoryTreeDto;
import imbuy.category.dto.PageResponse;
import imbuy.category.mapper.CategoryMapper;
import imbuy.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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

    private Mono<CategoryDto> buildCategoryWithChildren(Category category) {
        Mono<List<CategoryDto>> childrenMono = categoryRepository.findByParentId(category.getId())
                .flatMap(this::buildCategoryWithChildren)
                .collectList();
        
        Mono<String> parentNameMono = category.getParentId() != null
                ? categoryRepository.findById(category.getParentId())
                        .map(Category::getName)
                        .switchIfEmpty(Mono.just(""))
                : Mono.just("");
        
        return Mono.zip(childrenMono, parentNameMono)
                .map(tuple -> {
                    List<CategoryDto> children = tuple.getT1();
                    String parentName = tuple.getT2();
                    return categoryMapper.toDtoWithChildren(
                            category,
                            parentName.isEmpty() ? null : parentName,
                            children.isEmpty() ? null : children
                    );
                });
    }

    public Mono<PageResponse<CategoryDto>> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll()
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(categoryMapper::toDto)
                .collectList()
                .flatMap(categories -> {
                    return categoryRepository.count()
                            .map(total -> {
                                int pageNumber = pageable.getPageNumber();
                                int pageSize = pageable.getPageSize();
                                boolean hasNext = (pageNumber + 1) * pageSize < total;
                                boolean hasPrevious = pageNumber > 0;
                                
                                return new PageResponse<>(
                                        categories,
                                        pageNumber,
                                        pageSize,
                                        hasNext,
                                        hasPrevious
                                );
                            });
                });
    }

    public Mono<CategoryDto> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> {
                    Mono<String> parentNameMono = category.getParentId() != null
                            ? categoryRepository.findById(category.getParentId())
                                    .map(Category::getName)
                                    .switchIfEmpty(Mono.just(""))
                            : Mono.just("");
                    
                    Mono<List<CategoryDto>> childrenMono = categoryRepository.findByParentId(id)
                            .map(categoryMapper::toDto)
                            .collectList();
                    
                    return Mono.zip(parentNameMono, childrenMono)
                            .map(tuple -> categoryMapper.toDtoWithChildren(
                                    category,
                                    tuple.getT1().isEmpty() ? null : tuple.getT1(),
                                    tuple.getT2()
                            ));
                });
    }

    public Mono<CategoryDto> createCategory(CategoryDto categoryDto) {
        return categoryRepository.existsByNameAndParentId(categoryDto.name(), categoryDto.parent_id())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Category with this name already exists"));
                    }
                    
                    if (categoryDto.parent_id() != null) {
                        return categoryRepository.findById(categoryDto.parent_id())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Parent category not found")))
                                .then(createCategoryEntity(categoryDto));
                    }
                    
                    return createCategoryEntity(categoryDto);
                });
    }

    private Mono<CategoryDto> createCategoryEntity(CategoryDto categoryDto) {
        Category category = Category.builder()
                .name(categoryDto.name())
                .parentId(categoryDto.parent_id())
                .build();
        
        return categoryRepository.save(category)
                .map(categoryMapper::toDto);
    }

    public Mono<CategoryDto> updateCategory(Long id, CategoryDto categoryDto) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> {
                    if (categoryDto.parent_id() != null) {
                        return categoryRepository.findById(categoryDto.parent_id())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Parent category not found")))
                                .then(updateCategoryEntity(id, categoryDto));
                    }
                    return updateCategoryEntity(id, categoryDto);
                });
    }

    private Mono<CategoryDto> updateCategoryEntity(Long id, CategoryDto categoryDto) {
        return categoryRepository.findById(id)
                .map(category -> {
                    category.setName(categoryDto.name());
                    category.setParentId(categoryDto.parent_id());
                    return category;
                })
                .flatMap(categoryRepository::save)
                .map(categoryMapper::toDto);
    }

    public Mono<Void> deleteCategory(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")))
                .flatMap(category -> {
                    return categoryRepository.findByParentId(id)
                            .hasElements()
                            .flatMap(hasChildren -> {
                                if (Boolean.TRUE.equals(hasChildren)) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "Cannot delete category with subcategories"));
                                }
                                return categoryRepository.deleteById(id);
                            });
                });
    }
}