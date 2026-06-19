package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.dto.CsvImportResult;
import com.loanpro.ecommerce.domain.entity.Category;
import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.CategoryRepository;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductBatchSaver productBatchSaver;

    @InjectMocks
    private CsvImportService csvImportService;

    private MockMultipartFile createCsvFile(String content) {
        return new MockMultipartFile(
                "file", "products.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String csvHeader() {
        return "name,sku,description,category,price,stock,weight_kg";
    }

    @Test
    @DisplayName("importCsv with valid rows returns correct success count")
    void importCsv_withValidRows_returnsCorrectSuccessCount() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,Sports,10.00,50,0.5\n"
                + "Shoe,SKU-2,A shoe,Footwear,25.00,30,1.2\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CsvImportResult result = csvImportService.importProducts(createCsvFile(csv));

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(0);
        verify(productBatchSaver).saveBatch(anyList());
    }

    @Test
    @DisplayName("importCsv with missing required name field adds row to errors")
    void importCsv_withMissingRequiredField_addsRowToErrors() {
        String csv = csvHeader() + "\n"
                + ",SKU-1,A ball,Sports,10.00,50,0.5\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        CsvImportResult result = csvImportService.importProducts(createCsvFile(csv));

        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("importCsv with invalid price format adds row to errors")
    void importCsv_withInvalidPriceFormat_addsRowToErrors() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,Sports,not-a-number,50,0.5\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        CsvImportResult result = csvImportService.importProducts(createCsvFile(csv));

        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("importCsv with dollar sign in price strips it and parses correctly")
    void importCsv_withDollarSignInPrice_stripsAndParsesCorrectly() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,Sports,$19.99,50,0.5\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        AtomicReference<List<Product>> capturedBatch = new AtomicReference<>();
        doAnswer(inv -> {
            List<Product> products = inv.getArgument(0);
            capturedBatch.set(new ArrayList<>(products));
            return null;
        }).when(productBatchSaver).saveBatch(anyList());

        CsvImportResult result = csvImportService.importProducts(createCsvFile(csv));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isEqualTo(0);
        assertThat(capturedBatch.get()).hasSize(1);
        assertThat(capturedBatch.get().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    @DisplayName("importCsv with a new category auto-creates the category")
    void importCsv_withNewCategory_autoCreatesCategory() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,Sports,10.00,50,0.5\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findByNameIgnoreCase("Sports")).thenReturn(Optional.empty());
        Category newCategory = Category.builder().id(1L).name("Sports").build();
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);

        csvImportService.importProducts(createCsvFile(csv));

        verify(categoryRepository).save(argThat(cat -> cat.getName().equals("Sports")));
    }

    @Test
    @DisplayName("importCsv with existing category in different case reuses existing category")
    void importCsv_withExistingCategoryDifferentCase_reusesCategory() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,SPORTS,10.00,50,0.5\n";

        Category existing = Category.builder().id(1L).name("Sports").build();
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(List.of(existing));

        csvImportService.importProducts(createCsvFile(csv));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("importCsv with duplicate SKU in file adds second occurrence to errors")
    void importCsv_withDuplicateSkuInFile_addsSecondOccurrenceToErrors() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,Sports,10.00,50,0.5\n"
                + "Shoe,SKU-1,A shoe,Footwear,20.00,30,1.0\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CsvImportResult result = csvImportService.importProducts(createCsvFile(csv));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("importCsv with multiple errors in same row accumulates all errors")
    void importCsv_withMultipleErrorsInSameRow_accumulatesAllErrors() {
        String csv = csvHeader() + "\n"
                + "Ball,SKU-1,A ball,Sports,not-a-price,not-a-stock,0.5\n";

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        CsvImportResult result = csvImportService.importProducts(createCsvFile(csv));

        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getErrorCsvBytes()).isNotNull();

        String errorCsv = new String(result.getErrorCsvBytes(), StandardCharsets.UTF_8);
        assertThat(errorCsv).contains("price must be a valid number");
        assertThat(errorCsv).contains("stock must be a valid integer");
    }
}
