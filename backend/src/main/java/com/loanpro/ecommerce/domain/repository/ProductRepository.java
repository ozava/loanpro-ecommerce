package com.loanpro.ecommerce.domain.repository;

import com.loanpro.ecommerce.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryId(Long categoryId);
}