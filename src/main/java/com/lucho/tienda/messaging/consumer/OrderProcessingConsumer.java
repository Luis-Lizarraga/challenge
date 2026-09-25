package com.lucho.tienda.messaging.consumer;

import com.lucho.tienda.exception.OutOfStockException;
import com.lucho.tienda.messaging.OrderProcessingMessage;
import com.lucho.tienda.service.impl.CartErrorService;
import com.lucho.tienda.service.impl.OrderMetricsService;
import com.lucho.tienda.service.impl.OrderProcessingService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import static com.lucho.tienda.constant.KafkaConstants.ORDER_PROCESSING_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingConsumer {

    private final CartErrorService cartErrorService;
    private final OrderProcessingService orderProcessingService;
    private final OrderMetricsService orderMetricsService;

    @KafkaListener(
            topics = ORDER_PROCESSING_TOPIC,
            groupId = "order-processing-group"
    )
    @Retryable(
            retryFor = {
                    ObjectOptimisticLockingFailureException.class,
                    OptimisticLockException.class
            },
            maxAttemptsExpression = "${cart.processing.retry.max-attempts:5}",
            backoff = @Backoff(
                    delayExpression = "${cart.processing.retry.backoff-delay:150}",
                    multiplierExpression = "${cart.processing.retry.multiplier:2.0}"
            )
    )
    public void consume(OrderProcessingMessage message) {

        Long cartId = message.cartId();

        log.info(
                "Starting Kafka order processing for cart ID: {}",
                cartId
        );

        try {
            orderProcessingService.process(cartId);

            log.info(
                    "Finished Kafka order processing for cart ID: {}",
                    cartId
            );

        } catch (ObjectOptimisticLockingFailureException |
                 OptimisticLockException ex) {

            // Re-throw so Spring Retry can retry the processing.
            throw ex;

        } catch (OutOfStockException ex) {

            log.warn(
                    "Order cancelled due to insufficient stock for cart ID: {}. Reason: {}",
                    cartId,
                    ex.getMessage()
            );

            cartErrorService.markAsCancelled(
                    cartId,
                    ex.getMessage()
            );

            orderMetricsService.incrementCancelled();

        } catch (Exception ex) {

            log.error(
                    "Order processing failed for cart ID: {}",
                    cartId,
                    ex
            );

            cartErrorService.markAsFailed(
                    cartId,
                    ex.getMessage()
            );

            orderMetricsService.incrementFailed();
        }
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromConcurrencyFailure(
            ObjectOptimisticLockingFailureException ex,
            OrderProcessingMessage message) {

        String msg = String.format(
                "All retries exhausted due to massive concurrency for cart ID: %s. Marking as FAILED.",
                message.cartId()
        );

        log.error(msg);

        cartErrorService.markAsFailed(
                message.cartId(),
                msg
        );

        orderMetricsService.incrementFailed();
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromOptimisticLockFailure(
            OptimisticLockException ex,
            OrderProcessingMessage message) {

        String msg = String.format(
                "All retries exhausted due to optimistic locking for cart ID: %s. Marking as FAILED.",
                message.cartId()
        );

        log.error(msg);

        cartErrorService.markAsFailed(
                message.cartId(),
                msg
        );

        orderMetricsService.incrementFailed();
    }
}