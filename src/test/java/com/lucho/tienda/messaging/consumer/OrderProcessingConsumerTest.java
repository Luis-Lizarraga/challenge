package com.lucho.tienda.messaging.consumer;

import com.lucho.tienda.exception.OutOfStockException;
import com.lucho.tienda.messaging.OrderProcessingMessage;
import com.lucho.tienda.service.impl.CartErrorService;
import com.lucho.tienda.service.impl.OrderMetricsService;
import com.lucho.tienda.service.impl.OrderProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingConsumerTest {

    private static final Long CART_ID = 1L;

    @Mock
    private CartErrorService cartErrorService;

    @Mock
    private OrderProcessingService orderProcessingService;

    @Mock
    private OrderMetricsService orderMetricsService;

    @InjectMocks
    private OrderProcessingConsumer consumer;

    private OrderProcessingMessage message;

    @BeforeEach
    void setUp() {
        message = new OrderProcessingMessage(CART_ID);
    }

    @Test
    void consume_ProcessesOrderSuccessfully() {
        consumer.consume(message);

        verify(orderProcessingService).process(CART_ID);

        verifyNoInteractions(cartErrorService);
        verify(orderMetricsService, never()).incrementCancelled();
        verify(orderMetricsService, never()).incrementFailed();
    }

    @Test
    void consume_MarksAsCancelled_WhenOutOfStock() {
        OutOfStockException exception =
                new OutOfStockException("Insufficient stock");

        doThrow(exception)
                .when(orderProcessingService)
                .process(CART_ID);

        consumer.consume(message);

        verify(cartErrorService)
                .markAsCancelled(CART_ID, exception.getMessage());

        verify(orderMetricsService)
                .incrementCancelled();

        verify(orderMetricsService, never())
                .incrementFailed();
    }

    @Test
    void consume_MarksAsFailed_OnUnexpectedException() {
        RuntimeException exception =
                new RuntimeException("Unexpected processing error");

        doThrow(exception)
                .when(orderProcessingService)
                .process(CART_ID);

        consumer.consume(message);

        verify(cartErrorService)
                .markAsFailed(CART_ID, exception.getMessage());

        verify(orderMetricsService)
                .incrementFailed();

        verify(orderMetricsService, never())
                .incrementCancelled();
    }

    @Test
    void consume_RethrowsOptimisticLockException_ForRetry() {
        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException(
                        "Cart",
                        CART_ID
                );

        doThrow(exception)
                .when(orderProcessingService)
                .process(CART_ID);

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> consumer.consume(message)
        );

        verify(cartErrorService, never())
                .markAsFailed(anyLong(), anyString());

        verify(cartErrorService, never())
                .markAsCancelled(anyLong(), anyString());

        verify(orderMetricsService, never())
                .incrementFailed();

        verify(orderMetricsService, never())
                .incrementCancelled();
    }

    @Test
    void recoverFromConcurrencyFailure_MarksCartAsFailed() {
        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException(
                        "Cart",
                        CART_ID
                );

        consumer.recoverFromConcurrencyFailure(
                exception,
                message
        );

        verify(cartErrorService)
                .markAsFailed(
                        eq(CART_ID),
                        contains("All retries exhausted")
                );

        verify(orderMetricsService)
                .incrementFailed();
    }
}