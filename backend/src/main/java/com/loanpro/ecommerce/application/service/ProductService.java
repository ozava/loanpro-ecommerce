package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.dto.ProductRequest;
import com.loanpro.ecommerce.application.dto.ProductResponse;
import com.loanpro.ecommerce.application.exception.ResourceNotFoundException;
import com.loanpro.ecommerce.domain.entity.Category;
import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.CategoryRepository;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = mapToEntity(request, new Product());
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.findBySku(request.sku())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new com.loanpro.ecommerce.application.exception.DuplicateSkuException("SKU already exists: " + request.sku());
                    }
                });
        mapToEntity(request, product);
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.search(query, pageable)
                .map(ProductResponse::fromEntity);
    }

    private Category resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String normalized = categoryName.trim().substring(0, 1).toUpperCase()
                + categoryName.trim().substring(1).toLowerCase();
        return categoryRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(normalized).build()));
    }

    private Product mapToEntity(ProductRequest request, Product product) {
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setWeightKg(request.weightKg());
        product.setCategory(resolveCategory(request.categoryName()));
        return product;
    }
}
