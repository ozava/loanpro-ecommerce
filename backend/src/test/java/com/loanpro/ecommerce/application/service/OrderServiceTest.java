package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.dto.OrderItemRequest;
import com.loanpro.ecommerce.application.dto.OrderRequest;
import com.loanpro.ecommerce.application.dto.OrderResponse;
import com.loanpro.ecommerce.application.exception.EmptyOrderException;
import com.loanpro.ecommerce.application.exception.InsufficientStockException;
import com.loanpro.ecommerce.application.exception.PaymentFailedException;
import com.loanpro.ecommerce.application.exception.ResourceNotFoundException;
import com.loanpro.ecommerce.application.exception.UnsupportedPaymentMethodException;
import com.loanpro.ecommerce.application.port.out.PaymentPort;
import com.loanpro.ecommerce.application.port.out.PaymentResult;
import com.loanpro.ecommerce.domain.entity.Order;
import com.loanpro.ecommerce.domain.entity.OrderItem;
import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.OrderRepository;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentPortResolver paymentPortResolver;

    @Mock
    private PaymentPort paymentPort;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("createOrder with valid items returns completed order with correct total and deducted stock")
    void createOrder_withValidItems_returnsCompletedOrder() {
        Product product1 = Product.builder()
                .id(1L).name("Ball").sku("SKU-1").price(new BigDecimal("10.00")).stock(100).build();
        Product product2 = Product.builder()
                .id(2L).name("Shoe").sku("SKU-2").price(new BigDecimal("25.50")).stock(50).build();

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 3)),
                "stripe"
        );

        when(paymentPortResolver.resolve("stripe")).thenReturn(paymentPort);
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product1, product2));
        when(paymentPort.processPayment(any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "txn-123", null));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("96.50"));
        assertThat(response.items()).hasSize(2);

        assertThat(product1.getStock()).isEqualTo(98);
        assertThat(product2.getStock()).isEqualTo(47);
    }

    @Test
    @DisplayName("createOrder with duplicate product IDs aggregates quantities into a single order item")
    void createOrder_withDuplicateProductIds_aggregatesQuantities() {
        Product product = Product.builder()
                .id(1L).name("Ball").sku("SKU-1").price(new BigDecimal("10.00")).stock(100).build();

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(1L, 3)),
                "stripe"
        );

        when(paymentPortResolver.resolve("stripe")).thenReturn(paymentPort);
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(paymentPort.processPayment(any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "txn-123", null));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        assertThat(product.getStock()).isEqualTo(95);
    }

    @Test
    @DisplayName("createOrder with empty items list throws EmptyOrderException")
    void createOrder_withEmptyItems_throwsEmptyOrderException() {
        OrderRequest request = new OrderRequest(Collections.emptyList(), "stripe");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(EmptyOrderException.class);
    }

    @Test
    @DisplayName("createOrder with non-existent product throws ResourceNotFoundException")
    void createOrder_withNonExistentProduct_throwsResourceNotFoundException() {
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(999L, 1)),
                "stripe"
        );

        when(paymentPortResolver.resolve("stripe")).thenReturn(paymentPort);
        when(productRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("createOrder with insufficient stock throws InsufficientStockException and order is never saved")
    void createOrder_withInsufficientStock_throwsInsufficientStockException() {
        Product product = Product.builder()
                .id(1L).name("Ball").sku("SKU-1").price(new BigDecimal("10.00")).stock(2).build();

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 5)),
                "stripe"
        );

        when(paymentPortResolver.resolve("stripe")).thenReturn(paymentPort);
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder when payment fails throws PaymentFailedException, saves order as failed, and does not deduct stock")
    void createOrder_whenPaymentFails_throwsPaymentFailedExceptionAndStockNotDeducted() {
        Product product = Product.builder()
                .id(1L).name("Ball").sku("SKU-1").price(new BigDecimal("10.00")).stock(50).build();

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 2)),
                "stripe"
        );

        when(paymentPortResolver.resolve("stripe")).thenReturn(paymentPort);
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(paymentPort.processPayment(any(BigDecimal.class)))
                .thenReturn(new PaymentResult(false, null, "card declined"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("card declined");

        assertThat(product.getStock()).isEqualTo(50);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("failed");
    }

    @Test
    @DisplayName("createOrder with unsupported payment method throws UnsupportedPaymentMethodException before any product lookup")
    void createOrder_withUnsupportedPaymentMethod_throwsUnsupportedPaymentMethodException() {
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 1)),
                "bitcoin"
        );

        when(paymentPortResolver.resolve("bitcoin"))
                .thenThrow(new UnsupportedPaymentMethodException("bitcoin", Set.of("stripe", "paypal")));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(UnsupportedPaymentMethodException.class);

        verify(productRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("getOrder with existing ID returns order response")
    void getOrder_withExistingId_returnsOrderResponse() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("completed");
        order.setPaymentMethod("stripe");
        order.setTotalAmount(new BigDecimal("100.00"));

        Product product = Product.builder().id(10L).name("Ball").sku("SKU-10").build();
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        order.addItem(item);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(1L);

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("getOrder with non-existent ID throws ResourceNotFoundException")
    void getOrder_withNonExistentId_throwsResourceNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
