package com.lucho.tienda.service.impl;

import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    private static final Long CART_ID = 1L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StockService stockService;

    @Mock
    private OrderMetricsService orderMetricsService;

    @Mock
    private Timer.Sample timerSample;

    @InjectMocks
    private OrderProcessingService orderProcessingService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setId(1L);
        product.setCode("P01");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(10);

        cart = new Cart();
        cart.setId(CART_ID);
        cart.setStatus(CartStatus.PROCESSING);
        cart.setItems(new HashSet<>());

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .build();

        cart.getItems().add(item);
    }

    @Test
    void process_ProcessesCartAndRecordsMetrics_WhenCartIsProcessing() {
        when(cartRepository.findCartById(CART_ID))
                .thenReturn(Optional.of(cart));
        when(orderMetricsService.startTimer())
                .thenReturn(timerSample);

        orderProcessingService.process(CART_ID);

        assertEquals(CartStatus.PROCESSED, cart.getStatus());
        assertEquals(new BigDecimal("200.00"), cart.getTotalAmount());

        verify(stockService).deductStockForCart(cart);
        verify(orderMetricsService).incrementProcessed();
        verify(orderMetricsService).stopTimer(timerSample);
    }

    @Test
    void process_SkipsProcessingAndMetrics_WhenCartIsNotProcessing() {
        cart.setStatus(CartStatus.PROCESSED);

        when(cartRepository.findCartById(CART_ID))
                .thenReturn(Optional.of(cart));

        orderProcessingService.process(CART_ID);

        verifyNoInteractions(stockService);
        verifyNoInteractions(orderMetricsService);
    }

    @Test
    void process_ThrowsNotFoundAndDoesNotStartMetrics_WhenCartDoesNotExist() {
        when(cartRepository.findCartById(CART_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderProcessingService.process(CART_ID)
        );

        verifyNoInteractions(stockService);
        verifyNoInteractions(orderMetricsService);
    }

    @Test
    void process_StopsTimerButDoesNotIncrementProcessed_WhenProcessingFails() {
        when(cartRepository.findCartById(CART_ID))
                .thenReturn(Optional.of(cart));
        when(orderMetricsService.startTimer())
                .thenReturn(timerSample);

        doThrow(new RuntimeException("Stock processing error"))
                .when(stockService)
                .deductStockForCart(cart);

        assertThrows(
                RuntimeException.class,
                () -> orderProcessingService.process(CART_ID)
        );

        verify(orderMetricsService, never()).incrementProcessed();
        verify(orderMetricsService).stopTimer(timerSample);
    }
}
