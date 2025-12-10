package imbuy.category;

import imbuy.category.domain.Category;
import imbuy.category.dto.*;
import imbuy.category.mapper.CategoryMapper;
import imbuy.category.repository.CategoryRepository;
import imbuy.category.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class CategoryServiceApplicationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("category_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    private Category parentCategory;
    private Category childCategory1;
    private Category childCategory2;

    @BeforeEach
    void beforeEach() {
        categoryRepository.deleteAll().block();

        parentCategory = Category.builder()
                .name("Electronics")
                .parentId(null)
                .build();

        parentCategory = categoryRepository.save(parentCategory).block();

        childCategory1 = Category.builder()
                .name("Laptops")
                .parentId(parentCategory.getId())
                .build();

        childCategory2 = Category.builder()
                .name("Phones")
                .parentId(parentCategory.getId())
                .build();

        childCategory1 = categoryRepository.save(childCategory1).block();
        childCategory2 = categoryRepository.save(childCategory2).block();
    }

    @Test
    void getCategoryTree_shouldReturnHierarchy() {
        CategoryTreeDto tree = categoryService.getCategoryTree().block();

        assertNotNull(tree);
        assertNotNull(tree.categories());
        assertEquals(1, tree.categories().size());

        CategoryTreeDto.CategoryNode root = tree.categories().get(0);
        assertEquals("Electronics", root.name());
        assertNull(root.parentId());
        assertNotNull(root.children());
        assertEquals(2, root.children().size());

        List<String> childNames = root.children().stream()
                .map(CategoryTreeDto.CategoryNode::name)
                .toList();
        assertTrue(childNames.contains("Laptops"));
        assertTrue(childNames.contains("Phones"));
    }

    @Test
    void getAllCategories_shouldReturnPaginated() {
        List<CategoryResponse> result = categoryService.getAllCategories(
                        PageRequest.of(0, 10))
                .collectList()
                .block();

        assertNotNull(result);
        assertEquals(3, result.size());

        List<String> categoryNames = result.stream()
                .map(CategoryResponse::name)
                .toList();
        assertTrue(categoryNames.contains("Electronics"));
        assertTrue(categoryNames.contains("Laptops"));
        assertTrue(categoryNames.contains("Phones"));
    }

    @Test
    void getAllCategories_shouldReturnCorrectPageSize() {
        for (int i = 0; i < 7; i++) {
            Category category = Category.builder()
                    .name("Category " + i)
                    .parentId(null)
                    .build();
            categoryRepository.save(category).block();
        }

        List<CategoryResponse> page1 = categoryService.getAllCategories(
                        PageRequest.of(0, 5))
                .collectList()
                .block();

        List<CategoryResponse> page2 = categoryService.getAllCategories(
                        PageRequest.of(1, 5))
                .collectList()
                .block();

        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(5, page1.size());
        assertEquals(5, page2.size());

        List<Long> page1Ids = page1.stream().map(CategoryResponse::id).toList();
        List<Long> page2Ids = page2.stream().map(CategoryResponse::id).toList();
        assertTrue(page1Ids.stream().noneMatch(page2Ids::contains));
    }

    @Test
    void getCategoryById_shouldReturnWithChildren() {
        CategoryResponse result = categoryService.getCategoryById(parentCategory.getId()).block();

        assertNotNull(result);
        assertEquals("Electronics", result.name());
        assertNull(result.parentId());
        assertNotNull(result.children());
        assertEquals(2, result.children().size());
    }

    @Test
    void getCategoryById_shouldThrowWhenNotFound() {
        StepVerifier.create(categoryService.getCategoryById(999L))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Category not found"))
                .verify();
    }

    @Test
    void createCategory_shouldCreateNewCategory() {
        CategoryRequest request = new CategoryRequest("Tablets", parentCategory.getId());

        CategoryResponse result = categoryService.createCategory(request).block();

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Tablets", result.name());
        assertEquals(parentCategory.getId(), result.parentId());

        Category saved = categoryRepository.findById(result.id()).block();
        assertNotNull(saved);
        assertEquals("Tablets", saved.getName());
    }

    @Test
    void createCategory_shouldThrowWhenDuplicateName() {
        CategoryRequest request = new CategoryRequest("Electronics", null);

        StepVerifier.create(categoryService.createCategory(request))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Category with this name already exists"))
                .verify();
    }

    @Test
    void createCategory_shouldThrowWhenParentNotFound() {
        CategoryRequest request = new CategoryRequest("New Category", 999L);

        StepVerifier.create(categoryService.createCategory(request))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Parent category not found"))
                .verify();
    }

    @Test
    void updateCategory_shouldUpdateFields() {
        CategoryRequest request = new CategoryRequest("Updated Electronics", null);

        CategoryResponse result = categoryService.updateCategory(parentCategory.getId(), request).block();

        assertNotNull(result);
        assertEquals("Updated Electronics", result.name());
        assertNull(result.parentId());

        Category updated = categoryRepository.findById(parentCategory.getId()).block();
        assertNotNull(updated);
        assertEquals("Updated Electronics", updated.getName());
    }

    @Test
    void updateCategory_shouldThrowWhenNotFound() {
        CategoryRequest request = new CategoryRequest("Updated", null);

        StepVerifier.create(categoryService.updateCategory(999L, request))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Category not found"))
                .verify();
    }

    @Test
    void updateCategory_shouldValidateParent() {
        CategoryRequest request = new CategoryRequest("Updated", 999L);

        StepVerifier.create(categoryService.updateCategory(parentCategory.getId(), request))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Parent category not found"))
                .verify();
    }

    @Test
    void deleteCategory_shouldDeleteWhenNoChildren() {
        Category leafCategory = Category.builder()
                .name("Accessories")
                .parentId(parentCategory.getId())
                .build();
        leafCategory = categoryRepository.save(leafCategory).block();

        assertNotNull(leafCategory);
        categoryService.deleteCategory(leafCategory.getId()).block();

        Boolean exists = categoryRepository.existsById(leafCategory.getId()).block();
        assertFalse(exists);
    }

    @Test
    void deleteCategory_shouldThrowWhenHasChildren() {
        StepVerifier.create(categoryService.deleteCategory(parentCategory.getId()))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Cannot delete category with subcategories"))
                .verify();
    }

    @Test
    void deleteCategory_shouldThrowWhenNotFound() {
        StepVerifier.create(categoryService.deleteCategory(999L))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains("Category not found"))
                .verify();
    }

    @Test
    void categoryRepository_findRootCategories_shouldWork() {
        List<Category> roots = categoryRepository.findRootCategories()
                .collectList()
                .block();

        assertNotNull(roots);
        assertEquals(1, roots.size());
        assertEquals("Electronics", roots.get(0).getName());
    }

    @Test
    void categoryRepository_findByParentId_shouldWork() {
        List<Category> children = categoryRepository.findByParentId(parentCategory.getId())
                .collectList()
                .block();

        assertNotNull(children);
        assertEquals(2, children.size());

        List<String> childNames = children.stream()
                .map(Category::getName)
                .toList();
        assertTrue(childNames.contains("Laptops"));
        assertTrue(childNames.contains("Phones"));
    }

    @Test
    void categoryRepository_existsByNameAndParentId_shouldWork() {
        Boolean exists = categoryRepository.existsByNameAndParentId("Electronics", null)
                .block();
        Boolean notExists = categoryRepository.existsByNameAndParentId("TVs", null)
                .block();

        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void categoryRepository_saveAndFind_shouldWork() {
        Category newCategory = Category.builder()
                .name("Monitors")
                .parentId(parentCategory.getId())
                .build();

        Category saved = categoryRepository.save(newCategory).block();
        Category found = categoryRepository.findById(saved.getId()).block();

        assertNotNull(found);
        assertEquals("Monitors", found.getName());
        assertEquals(parentCategory.getId(), found.getParentId());
    }

    @Test
    void categoryMapper_toEntity_shouldMapRequest() {
        CategoryRequest request = new CategoryRequest("Test", parentCategory.getId());
        Category entity = categoryMapper.toEntity(request);

        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals("Test", entity.getName());
        assertEquals(parentCategory.getId(), entity.getParentId());
    }

    @Test
    void categoryMapper_toResponse_shouldMapBasicFields() {
        CategoryResponse response = categoryMapper.toResponse(parentCategory);

        assertNotNull(response);
        assertEquals(parentCategory.getId(), response.id());
        assertEquals("Electronics", response.name());
        assertNull(response.parentId());
        assertNull(response.children());
    }

    @Test
    void categoryMapper_toResponseWithChildren_shouldMapHierarchy() {
        List<CategoryResponse> children = List.of(
                new CategoryResponse(childCategory1.getId(), "Laptops", parentCategory.getId(), null),
                new CategoryResponse(childCategory2.getId(), "Phones", parentCategory.getId(), null)
        );

        CategoryResponse response = categoryMapper.toResponseWithChildren(parentCategory, children);

        assertNotNull(response);
        assertEquals(parentCategory.getId(), response.id());
        assertEquals("Electronics", response.name());
        assertNull(response.parentId());
        assertNotNull(response.children());
        assertEquals(2, response.children().size());
        assertEquals("Laptops", response.children().get(0).name());
        assertEquals("Phones", response.children().get(1).name());
    }

    @Test
    void completeCategoryFlow_shouldWork() {
        CategoryRequest rootRequest = new CategoryRequest("Furniture", null);
        CategoryResponse root = categoryService.createCategory(rootRequest).block();
        assertNotNull(root);
        assertEquals("Furniture", root.name());

        CategoryRequest childRequest = new CategoryRequest("Chairs", root.id());
        CategoryResponse child = categoryService.createCategory(childRequest).block();
        assertNotNull(child);
        assertEquals("Chairs", child.name());
        assertEquals(root.id(), child.parentId());

        CategoryTreeDto tree = categoryService.getCategoryTree().block();
        assertNotNull(tree);
        assertEquals(2, tree.categories().size());

        CategoryTreeDto.CategoryNode furnitureInTree = tree.categories().stream()
                .filter(c -> "Furniture".equals(c.name()))
                .findFirst()
                .orElseThrow();
        assertNotNull(furnitureInTree.children());
        assertEquals(1, furnitureInTree.children().size());
        assertEquals("Chairs", furnitureInTree.children().get(0).name());

        CategoryRequest updateRequest = new CategoryRequest("Updated Furniture", null);
        CategoryResponse updated = categoryService.updateCategory(root.id(), updateRequest).block();
        assertEquals("Updated Furniture", updated.name());

        CategoryResponse byId = categoryService.getCategoryById(root.id()).block();
        assertEquals("Updated Furniture", byId.name());
        assertNotNull(byId.children());
        assertEquals(1, byId.children().size());

        categoryService.deleteCategory(child.id()).block();

        Boolean childExists = categoryRepository.existsById(child.id()).block();
        assertFalse(childExists);

        categoryService.deleteCategory(root.id()).block();

        Boolean rootExists = categoryRepository.existsById(root.id()).block();
        assertFalse(rootExists);
    }

    @Test
    void deepHierarchy_shouldBuildCorrectTree() {
        Category level1 = categoryRepository.save(
                Category.builder().name("Level1").parentId(null).build()
        ).block();

        Category level2 = categoryRepository.save(
                Category.builder().name("Level2").parentId(level1.getId()).build()
        ).block();

        Category level3 = categoryRepository.save(
                Category.builder().name("Level3").parentId(level2.getId()).build()
        ).block();

        CategoryTreeDto tree = categoryService.getCategoryTree().block();

        assertNotNull(tree);
        assertEquals(2, tree.categories().size());

        CategoryTreeDto.CategoryNode level1Dto = tree.categories().stream()
                .filter(c -> "Level1".equals(c.name()))
                .findFirst()
                .orElseThrow();

        assertNotNull(level1Dto.children());
        assertEquals(1, level1Dto.children().size());
        assertEquals("Level2", level1Dto.children().get(0).name());

        assertNotNull(level1Dto.children().get(0).children());
        assertEquals(1, level1Dto.children().get(0).children().size());
        assertEquals("Level3", level1Dto.children().get(0).children().get(0).name());
    }

    @Test
    void pagination_shouldWorkCorrectly() {
        for (int i = 0; i < 15; i++) {
            Category category = Category.builder()
                    .name("Category " + i)
                    .parentId(null)
                    .build();
            categoryRepository.save(category).block();
        }

        List<CategoryResponse> page1 = categoryService.getAllCategories(
                        PageRequest.of(0, 10))
                .collectList()
                .block();

        List<CategoryResponse> page2 = categoryService.getAllCategories(
                        PageRequest.of(1, 10))
                .collectList()
                .block();

        assertNotNull(page1);
        assertEquals(10, page1.size());

        assertNotNull(page2);
        assertEquals(8, page2.size());

        List<Long> page1Ids = page1.stream().map(CategoryResponse::id).toList();
        List<Long> page2Ids = page2.stream().map(CategoryResponse::id).toList();
        assertTrue(page1Ids.stream().noneMatch(page2Ids::contains));
    }

    @Test
    void updateEntity_shouldUpdateFields() {
        Category existingCategory = parentCategory;
        CategoryRequest request = new CategoryRequest("Updated Name", 999L);

        categoryMapper.updateEntity(request, existingCategory);

        assertEquals("Updated Name", existingCategory.getName());
        assertEquals(999L, existingCategory.getParentId());
    }

    @Test
    void getAllCategories_withLargePageSize_shouldRespectMaxSize() {
        List<CategoryResponse> result = categoryService.getAllCategories(
                        PageRequest.of(0, 100))
                .collectList()
                .block();

        assertNotNull(result);
        assertTrue(result.size() <= 3);
    }
}