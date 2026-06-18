package com.loanpro.ecommerce.infrastructure.web;

import com.loanpro.ecommerce.application.dto.CsvImportResult;
import com.loanpro.ecommerce.application.dto.ProductRequest;
import com.loanpro.ecommerce.application.dto.ProductResponse;
import com.loanpro.ecommerce.application.exception.ResourceNotFoundException;
import com.loanpro.ecommerce.application.service.CsvImportService;
import com.loanpro.ecommerce.application.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CsvImportService csvImportService;

    public ProductController(ProductService productService, CsvImportService csvImportService) {
        this.productService = productService;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam("q") String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) {
        CsvImportResult result = csvImportService.importProducts(file);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("totalRows", result.getTotalRows());
        response.put("successCount", result.getSuccessCount());
        response.put("errorCount", result.getErrorCount());
        if (result.getErrorFileId() != null) {
            response.put("errorFileId", result.getErrorFileId());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/import/errors/{errorFileId}")
    public ResponseEntity<byte[]> downloadErrorFile(@PathVariable String errorFileId) {
        byte[] errorCsv = csvImportService.getErrorFile(errorFileId);
        if (errorCsv == null) {
            throw new ResourceNotFoundException("Error file not found or expired: " + errorFileId);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "import-errors.csv");
        return ResponseEntity.ok().headers(headers).body(errorCsv);
    }
}