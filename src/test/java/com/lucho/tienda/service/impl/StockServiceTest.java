package com.lucho.tienda.service.impl;

import com.lucho.tienda.exception.OutOfStockException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.repository.ProductRepository;
import com.lucho.tienda.strategy.DatabaseDiscountStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DatabaseDiscountStrategy discountStrategy;

    @InjectMocks
    private StockService stockService;

    private Cart cart;
    private CartItem item;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setCode("P01");
        product.setPrice(new BigDecimal("1000.00"));
        product.setStock(10);

        item = new CartItem();
        item.setId(1L);
        item.setProduct(product);
        item.setQuantity(2);

        cart = new Cart();
        cart.setId(1L);
        cart.setItems(new HashSet<>());
        cart.getItems().add(item);
    }

    @Test
    void deductStockForCart_Success() {
        // Arrange
        when(discountStrategy.getActiveDiscountsMap()).thenReturn(Map.of());
        when(productRepository.decrementStockSafely("P01", 2)).thenReturn(1);
        when(discountStrategy.calculateItemDiscount(eq(item), anyMap())).thenReturn(BigDecimal.ZERO);

        // Act
        assertDoesNotThrow(() -> stockService.deductStockForCart(cart));

        // Assert
        assertEquals(new BigDecimal("1000.00"), item.getUnitPrice());
        assertEquals(BigDecimal.ZERO, item.getDiscountAmount());
        verify(productRepository).decrementStockSafely("P01", 2);
    }

    @Test
    void deductStockForCart_ThrowsOutOfStockException_WhenStockDecrementFails() {
        // Arrange
        when(discountStrategy.getActiveDiscountsMap()).thenReturn(Map.of());
        when(productRepository.decrementStockSafely("P01", 2)).thenReturn(0); // 0 filas actualizadas = Sin stock

        // Act & Assert
        assertThrows(OutOfStockException.class, () -> stockService.deductStockForCart(cart));
        verify(productRepository).decrementStockSafely("P01", 2);
    }

    @Test
    void deductStockForCart_DoesNothing_WhenCartOrItemsAreNullOrEmpty() {
        // Act & Assert
        assertDoesNotThrow(() -> stockService.deductStockForCart(null));
        assertDoesNotThrow(() -> stockService.deductStockForCart(new Cart()));

        verifyNoInteractions(productRepository);
        verifyNoInteractions(discountStrategy);
    }
}