package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.dto.ProductRequest;
import com.loanpro.ecommerce.application.dto.ProductResponse;
import com.loanpro.ecommerce.application.exception.DuplicateSkuException;
import com.loanpro.ecommerce.application.exception.ResourceNotFoundException;
import com.loanpro.ecommerce.domain.entity.Category;
import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.CategoryRepository;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest buildRequest(String name, String sku, String categoryName) {
        return new ProductRequest(name, sku, null, categoryName,
                new BigDecimal("29.99"), 100, null);
    }

    @Test
    @DisplayName("createProduct with a new category name creates and saves a new category")
    void createProduct_withNewCategoryName_createsNewCategory() {
        ProductRequest request = buildRequest("Ball", "SKU-1", "Sports");

        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCase("Sports")).thenReturn(Optional.empty());
        Category savedCategory = Category.builder().id(1L).name("Sports").build();
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        verify(categoryRepository).save(argThat(cat -> cat.getName().equals("Sports")));
        assertThat(response.categoryName()).isEqualTo("Sports");
    }

    @Test
    @DisplayName("createProduct with existing category in different case reuses existing category without creating a duplicate")
    void createProduct_withExistingCategoryDifferentCase_reusesExistingCategory() {
        ProductRequest request = buildRequest("Ball", "SKU-1", "BASKETBALL");

        Category existing = Category.builder().id(1L).name("Basketball").build();
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCase("Basketball")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        verify(categoryRepository, never()).save(any(Category.class));
        assertThat(response.categoryName()).isEqualTo("Basketball");
    }

    @Test
    @DisplayName("createProduct normalizes new category name to title case")
    void createProduct_normalizesNewCategoryName() {
        ProductRequest request = buildRequest("Ball", "SKU-1", "BASKETBALL");

        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCase("Basketball")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        productService.createProduct(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Basketball");
    }

    @Test
    @DisplayName("createProduct with duplicate SKU throws DuplicateSkuException")
    void createProduct_withDuplicateSku_throwsDuplicateSkuException() {
        ProductRequest request = buildRequest("Ball", "SKU-1", "Sports");

        Product existing = Product.builder().id(99L).sku("SKU-1").build();
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-1");
    }

    @Test
    @DisplayName("updateProduct with existing ID updates all fields correctly")
    void updateProduct_withExistingId_updatesFields() {
        Product existing = Product.builder()
                .id(1L).name("Old").sku("SKU-OLD")
                .price(new BigDecimal("10.00")).stock(10).build();
        ProductRequest request = new ProductRequest("New", "SKU-NEW", "desc", null,
                new BigDecimal("20.00"), 50, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.findBySku("SKU-NEW")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.sku()).isEqualTo("SKU-NEW");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(response.stock()).isEqualTo(50);
    }

    @Test
    @DisplayName("updateProduct with non-existent ID throws ResourceNotFoundException")
    void updateProduct_withNonExistentId_throwsResourceNotFoundException() {
        ProductRequest request = buildRequest("Ball", "SKU-1", "Sports");

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProduct changing to an existing SKU of a different product throws DuplicateSkuException")
    void updateProduct_changingToExistingSku_throwsDuplicateSkuException() {
        Product current = Product.builder().id(1L).name("A").sku("SKU-A")
                .price(new BigDecimal("10.00")).stock(10).build();
        Product other = Product.builder().id(2L).sku("SKU-B").build();
        ProductRequest request = buildRequest("A", "SKU-B", null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(current));
        when(productRepository.findBySku("SKU-B")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> productService.updateProduct(1L, request))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-B");
    }

    @Test
    @DisplayName("deleteProduct with existing ID deletes successfully")
    void deleteProduct_withExistingId_deletesSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProduct with non-existent ID throws ResourceNotFoundException")
    void deleteProduct_withNonExistentId_throwsResourceNotFoundException() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
