package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.dto.OrderItemRequest;
import com.loanpro.ecommerce.application.dto.OrderRequest;
import com.loanpro.ecommerce.application.dto.OrderResponse;
import com.loanpro.ecommerce.application.exception.EmptyOrderException;
import com.loanpro.ecommerce.application.exception.InsufficientStockException;
import com.loanpro.ecommerce.application.exception.PaymentFailedException;
import com.loanpro.ecommerce.application.exception.ResourceNotFoundException;
import com.loanpro.ecommerce.application.port.out.PaymentPort;
import com.loanpro.ecommerce.application.port.out.PaymentResult;
import com.loanpro.ecommerce.domain.entity.Order;
import com.loanpro.ecommerce.domain.entity.OrderItem;
import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.OrderRepository;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentPortResolver paymentPortResolver;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        PaymentPortResolver paymentPortResolver) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentPortResolver = paymentPortResolver;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new EmptyOrderException();
        }

        PaymentPort paymentPort = paymentPortResolver.resolve(request.paymentMethod());

        Map<Long, Integer> aggregated = request.items().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum
                ));

        List<Long> productIds = new ArrayList<>(aggregated.keySet());
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            List<Long> foundIds = products.stream().map(Product::getId).toList();
            List<Long> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ResourceNotFoundException("Products not found: " + missingIds);
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
            Product product = productMap.get(entry.getKey());
            if (product.getStock() < entry.getValue()) {
                throw new InsufficientStockException(entry.getKey(), entry.getValue(), product.getStock());
            }
        }

        Order order = new Order();
        order.setStatus("pending");
        order.setPaymentMethod(request.paymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
            Product product = productMap.get(entry.getKey());
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(entry.getValue());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            order.addItem(item);
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        order.setTotalAmount(totalAmount);

        PaymentResult result = paymentPort.processPayment(totalAmount);

        if (!result.success()) {
            order.setStatus("failed");
            orderRepository.save(order);
            throw new PaymentFailedException(result.failureReason());
        }

        for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
            Product product = productMap.get(entry.getKey());
            product.setStock(product.getStock() - entry.getValue());
        }

        order.setStatus("completed");
        orderRepository.save(order);

        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderResponse.fromEntity(order);
    }
}
