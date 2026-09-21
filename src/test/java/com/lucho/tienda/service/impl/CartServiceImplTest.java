package com.lucho.tienda.service.impl;

import com.lucho.tienda.dto.ProductOperationRequest;
import com.lucho.tienda.event.OrderProcessingEvent;
import com.lucho.tienda.exception.BadRequestException;
import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.model.*;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import com.lucho.tienda.repository.ProductRepository;
import com.lucho.tienda.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(1L);
        product.setCode("P01");
        product.setName("Whey Protein");
        product.setPrice(new BigDecimal("1000.00"));
        product.setCategory(new Category());
        product.setStock(10);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setStatus(CartStatus.CREATED); // Default valid state
        cart.setItems(new HashSet<>());

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);

        cart.getItems().add(cartItem);

        lenient().when(cartRepository.save(any(Cart.class))).thenReturn(cart);
    }

    // --- CREATE CART ---

    @Test
    void createCart_Success_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Cart created = cartService.createCart(1L);

        assertNotNull(created);
        assertEquals(1L, created.getUser().getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createCart_ThrowsBadRequest_WhenUserIdIsNull() {
        assertThrows(BadRequestException.class, () -> cartService.createCart(null));
    }

    @Test
    void createCart_ThrowsNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cartService.createCart(1L));
    }

    // --- ADD PRODUCT ---

    //@Test
    void addProduct_IncreasesQuantity_WhenProductAlreadyInCart() {
        ProductOperationRequest request = new ProductOperationRequest(1L, "P01", 1);
        when(cartRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByCode("P01")).thenReturn(Optional.of(product));

        Cart updatedCart = cartService.addProduct(1L, request);

        assertEquals(1, updatedCart.getItems().size());
        assertEquals(2, updatedCart.getItems().stream().findFirst().orElseThrow().getQuantity());
        verify(cartRepository).save(cart);
    }

    //@Test
    void addProduct_AddNewItem_WhenProductNotInCart() {
        Product newProduct = new Product();
        newProduct.setCode("P02");
        newProduct.setPrice(new BigDecimal("500.00"));
        newProduct.setStock(10);

        ProductOperationRequest request = new ProductOperationRequest(1L, "P02", 1);
        when(cartRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByCode("P02")).thenReturn(Optional.of(newProduct));

        Cart updatedCart = cartService.addProduct(1L, request);

        assertEquals(2, updatedCart.getItems().size());
        verify(cartRepository).save(cart);
    }

    @Test
    void addProduct_ThrowsException_OnNullRequest() {
        assertThrows(NullPointerException.class, () -> cartService.addProduct(1L, null));
    }

    @Test
    void addProduct_ThrowsNotFound_WhenProductDoesNotExist() {
        ProductOperationRequest request = new ProductOperationRequest(1L, "P99", 1);
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByCode("P99")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addProduct(1L, request));
    }

    //@Test
    void addProduct_ThrowsNotFound_WhenCartDoesNotExist() {
        ProductOperationRequest request = new ProductOperationRequest(99L, "P01", 1);
        when(cartRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addProduct(1L, request));
    }

    //@Test
    void addProduct_ThrowsBadRequest_WhenInsufficientStock() {
        product.setStock(1);
        ProductOperationRequest request = new ProductOperationRequest(1L, "P01", 1);
        when(cartRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByCode("P01")).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> cartService.addProduct(1L, request));

        verify(cartRepository, never()).save(any(Cart.class));
    }

    // --- UPDATE PRODUCT QUANTITY ---

    //@Test
    void updateProductQuantity_Success_WhenValidQuantity() {
        ProductOperationRequest request = new ProductOperationRequest(1L, "P01", 5);
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        Cart updatedCart = cartService.updateProductQuantity(1L, request);

        assertEquals(1, updatedCart.getItems().size());
        assertEquals(5, updatedCart.getItems().stream().findFirst().orElseThrow().getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateProductQuantity_ThrowsBadRequest_WhenQuantityIsNull() {
        ProductOperationRequest request = new ProductOperationRequest(1L, "P01", null);
        assertThrows(BadRequestException.class, () -> cartService.updateProductQuantity(1L, request));
    }

    @Test
    void updateProductQuantity_ThrowsBadRequest_WhenQuantityLessThanOne() {
        ProductOperationRequest request = new ProductOperationRequest(1L, "P01", 0);
        assertThrows(BadRequestException.class, () -> cartService.updateProductQuantity(1L, request));
    }

    @Test
    void updateProductQuantity_ThrowsBadRequest_WhenExceedsStock() {
        product.setStock(3);
        ProductOperationRequest request = new ProductOperationRequest(1L, "P01", 5);
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () -> cartService.updateProductQuantity(1L, request));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateProductQuantity_ThrowsNotFound_WhenProductNotInCart() {
        ProductOperationRequest request = new ProductOperationRequest(1L, "P99", 2);
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        assertThrows(ResourceNotFoundException.class, () -> cartService.updateProductQuantity(1L, request));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // --- REMOVE PRODUCT (DELETE) ---

    //@Test
    void removeProduct_RemovesItemCompletely_WhenProductInCart() {
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        Cart updatedCart = cartService.removeProduct(1L, 1L, "P01");

        assertTrue(updatedCart.getItems().isEmpty());
        verify(cartRepository).saveAndFlush(cart);
    }

    @Test
    void removeProduct_ThrowsNotFound_WhenProductNotInCart() {
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        assertThrows(ResourceNotFoundException.class, () -> cartService.removeProduct(1L, 1L, "NON-EXISTENT"));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeProduct_ThrowsNotFound_WhenCartDoesNotExist() {
        when(cartRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.removeProduct(1L, 99L, "P01"));
    }

    // --- GETTERS ---

    @Test
    void getCartProducts_ReturnsCartItems() {
        when(cartRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cart));
        List<CartItem> items = cartService.getCartProducts(1L, 1L);
        assertFalse(items.isEmpty());
    }

    @Test
    void getCartProducts_ThrowsBadRequest_WhenIdNull() {
        assertThrows(BadRequestException.class, () -> cartService.getCartProducts(1L, null));
    }

    @Test
    void getCartProducts_ThrowsNotFound_WhenCartDoesNotExist() {
        when(cartRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cartService.getCartProducts(1L, 99L));
    }

    @Test
    void getUserCarts_Success_WithoutStatusFilter() {
        when(cartRepository.findByUserId(1L)).thenReturn(List.of(cart));
        List<Cart> carts = cartService.getUserCarts(1L, null);
        assertEquals(1, carts.size());
    }

    @Test
    void getUserCarts_Success_WithStatusFilter() {
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.CREATED)).thenReturn(List.of(cart));
        List<Cart> carts = cartService.getUserCarts(1L, CartStatus.CREATED);
        assertEquals(1, carts.size());
    }

    @Test
    void getUserCarts_ReturnsEmptyList_WhenNoCartsFound() {
        when(cartRepository.findByUserId(2L)).thenReturn(List.of());
        List<Cart> carts = cartService.getUserCarts(2L, null);
        assertTrue(carts.isEmpty());
    }

    @Test
    void getUserCarts_ThrowsBadRequest_WhenIdNull() {
        assertThrows(BadRequestException.class, () -> cartService.getUserCarts(null, CartStatus.PROCESSED));
    }

    @Test
    void getCartById_Success() {
        when(cartRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cart));
        Cart found = cartService.getCartById(1L, 1L);
        assertNotNull(found);
    }

    @Test
    void getCartById_ThrowsBadRequest_WhenIdNull() {
        assertThrows(BadRequestException.class, () -> cartService.getCartById(1L, null));
    }

    @Test
    void getCartById_ThrowsNotFound_WhenCartDoesNotExist() {
        when(cartRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cartService.getCartById(1L, 99L));
    }

    // --- ORCHESTRATION & CHECKOUT ---

    @Test
    void initiateCheckout_Success_LocksCartAndPublishesEvent() {
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        cartService.initiateCheckout(1L, 1L);

        // Verify status changed to PROCESSING
        assertEquals(CartStatus.PROCESSING, cart.getStatus());

        // Verify application event was published
        verify(eventPublisher).publishEvent(any(OrderProcessingEvent.class));
    }

    @Test
    void initiateCheckout_ThrowsBadRequest_WhenCartAlreadyProcessing() {
        cart.setStatus(CartStatus.PROCESSING);
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () -> cartService.initiateCheckout(1L, 1L));

        // Event must never be published if state is invalid
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void initiateCheckout_ThrowsBadRequest_WhenCartIsAlreadyProcessed() {
        cart.setStatus(CartStatus.PROCESSED);
        when(cartRepository.findByIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () -> cartService.initiateCheckout(1L, 1L));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void initiateCheckout_ThrowsNotFound_WhenCartDoesNotExist() {
        when(cartRepository.findByIdAndUserIdForUpdate(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.initiateCheckout(1L, 99L));
    }
}