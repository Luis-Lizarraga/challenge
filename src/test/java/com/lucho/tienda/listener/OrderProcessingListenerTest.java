package com.lucho.tienda.listener;

import com.lucho.tienda.event.OrderProcessingEvent;
import com.lucho.tienda.exception.BadRequestException;
import com.lucho.tienda.exception.OutOfStockException;
import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import com.lucho.tienda.service.impl.CartErrorService;
import com.lucho.tienda.service.impl.StockService;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingListenerTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartErrorService cartErrorService;

    @Mock
    private StockService stockService;

    @InjectMocks
    private OrderProcessingListener listener;

    private Cart cart;
    private OrderProcessingEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderProcessingEvent(1L);

        Product product = new Product();
        product.setCode("P01");
        product.setPrice(new BigDecimal("100.00"));

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setDiscountAmount(BigDecimal.ZERO);

        Set<CartItem> items = new HashSet<>();
        items.add(item);

        cart = new Cart();
        cart.setId(1L);
        cart.setStatus(CartStatus.PROCESSING);
        cart.setItems(items);
    }

    @Test
    void processOrderAsync_Success_DeductsStockAndSavesCart() {
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));

        listener.processOrderAsync(event);

        verify(stockService).deductStockForCart(cart);
        verify(cartRepository).save(cart);
        assertEquals(CartStatus.PROCESSED, cart.getStatus());
        assertNotNull(cart.getTotalAmount());
    }

    @Test
    void processOrderAsync_HandlesNotFound_ByMarkingAsFailed_WhenCartDoesNotExist() {
        // Arrange
        when(cartRepository.findCartById(1L)).thenReturn(Optional.empty());

        // Act - Invoke the method, expecting it not to rethrow the exception
        listener.processOrderAsync(event);

        // Assert
        verify(stockService, never()).deductStockForCart(any());
        // Verify that lookup failure triggers the generic catch block behavior: mark cart as failed
        verify(cartErrorService).markCartAsFailed(1L);
    }

    @Test
    void processOrderAsync_SkipsExecution_WhenCartIsNotProcessing() {
        cart.setStatus(CartStatus.CREATED);
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));

        listener.processOrderAsync(event);

        // Ensures idempotency by skipping processing
        verify(stockService, never()).deductStockForCart(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void processOrderAsync_MarksAsFailed_WhenCartIsEmpty() {
        cart.setItems(new HashSet<>());
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));

        listener.processOrderAsync(event);

        verify(cartErrorService).markCartAsFailed(1L);
        verify(stockService, never()).deductStockForCart(any());
    }

    @Test
    void processOrderAsync_CatchesOutOfStock_MarksCancelledAndRollsBack() {
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));
        doThrow(new OutOfStockException("P01")).when(stockService).deductStockForCart(cart);

        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        // Mocks the static transaction context to prevent NoTransactionException during unit testing
        try (MockedStatic<TransactionAspectSupport> mockedStatic = mockStatic(TransactionAspectSupport.class)) {
            mockedStatic.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(transactionStatus);

            // Does not assertThrows because the listener catches and swallows the exception natively
            listener.processOrderAsync(event);

            verify(cartErrorService).markCartAsCancelled(1L);
            verify(transactionStatus).setRollbackOnly();
            verify(cartRepository, never()).save(cart);
        }
    }

    @Test
    void processOrderAsync_CatchesBadRequest_MarksFailedAndRollsBack() {
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));
        doThrow(new BadRequestException("Invalid Data")).when(stockService).deductStockForCart(cart);

        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        try (MockedStatic<TransactionAspectSupport> mockedStatic = mockStatic(TransactionAspectSupport.class)) {
            mockedStatic.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(transactionStatus);

            listener.processOrderAsync(event);

            verify(cartErrorService).markCartAsFailed(1L);
            verify(transactionStatus).setRollbackOnly();
        }
    }

    @Test
    void processOrderAsync_RethrowsOptimisticLockingFailure_ToTriggerRetry() {
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));
        doThrow(new ObjectOptimisticLockingFailureException(Cart.class, 1L))
                .when(stockService).deductStockForCart(cart);

        // Exception must be thrown out of the method to trigger @Retryable
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> listener.processOrderAsync(event));

        verify(cartErrorService, never()).markCartAsFailed(anyLong());
    }

    @Test
    void processOrderAsync_MarksAsFailed_OnUnexpectedException() {
        when(cartRepository.findCartById(1L)).thenReturn(Optional.of(cart));
        doThrow(new RuntimeException("Database down")).when(stockService).deductStockForCart(cart);

        listener.processOrderAsync(event);

        verify(cartErrorService).markCartAsFailed(1L);
    }

    @Test
    void recoverFromConcurrencyFailure_MarksAsFailed() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Cart.class, 1L);

        listener.recoverFromConcurrencyFailure(ex, event);

        verify(cartErrorService).markCartAsFailed(1L);
    }

    @Test
    void recoverFromStaleObject_MarksAsFailed() {
        StaleObjectStateException ex = new StaleObjectStateException("Cart", 1L);

        listener.recoverFromStaleObject(ex, event);

        verify(cartErrorService).markCartAsFailed(1L);
    }
}