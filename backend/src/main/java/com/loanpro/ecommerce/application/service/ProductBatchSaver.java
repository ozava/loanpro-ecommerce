package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProductBatchSaver {

    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public ProductBatchSaver(ProductRepository productRepository, EntityManager entityManager) {
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<Product> products) {
        log.info("Saving batch of {} products", products.size());
        productRepository.saveAll(products);
        productRepository.flush();
        entityManager.clear();
    }
}
