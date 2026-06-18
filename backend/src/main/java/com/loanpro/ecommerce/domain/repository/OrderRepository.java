package com.loanpro.ecommerce.domain.repository;

import com.loanpro.ecommerce.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
