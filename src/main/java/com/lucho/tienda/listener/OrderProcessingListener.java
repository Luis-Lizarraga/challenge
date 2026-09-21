package com.lucho.tienda.listener;

import com.lucho.tienda.constant.CartConstants;
import com.lucho.tienda.constant.ErrorMessageConstants;
import com.lucho.tienda.event.OrderProcessingEvent;
import com.lucho.tienda.exception.OutOfStockException;
import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.exception.BadRequestException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import com.lucho.tienda.service.impl.CartErrorService;
import com.lucho.tienda.service.impl.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingListener {

    private final CartRepository cartRepository;
    private final CartErrorService cartErrorService;
    private final StockService stockService;

    @Value("${cart.processing.sleep-ms:0}")
    private long sleepMs;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class, StaleObjectStateException.class},
            maxAttemptsExpression = "${cart.processing.retry.max-attempts:5}",
            backoff = @Backoff(
                    delayExpression = "${cart.processing.retry.backoff-delay:150}",
                    multiplierExpression = "${cart.processing.retry.multiplier:2.0}"
            )
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOrderAsync(OrderProcessingEvent event) {
        Long cartId = event.cartId();
        log.info("Starting asynchronous order processing for cart ID: {}", cartId);

        try {
            if (sleepMs > 0) {
                Thread.sleep(sleepMs);
            }

            Cart cart = cartRepository.findCartById(cartId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessageConstants.CART_NOT_FOUND, cartId)));

            // Defense 1: Idempotency. If another concurrent thread already processed this cart, abort silently.
            if (cart.getStatus() != CartStatus.PROCESSING) {
                log.info("Async execution skipped. Cart ID {} is already handled (Status: {}).", cartId, cart.getStatus());
                return;
            }

            // Defense 2: Security check before deducting stock.
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                log.warn("Cart ID {} is empty. Cancelling processing.", cartId);
                cartErrorService.markCartAsFailed(cartId);
                return;
            }

            // Delegate business rules.
            stockService.deductStockForCart(cart);

            // Finalize totals and status.
            BigDecimal accumulatedTotal = cart.getItems().stream()
                    .map(CartItem::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(CartConstants.MONEY_SCALE, RoundingMode.HALF_UP);

            cart.setTotalAmount(accumulatedTotal);
            cart.markAsProcessed();

            cartRepository.save(cart);
            log.info("Successfully processed order for cart ID: {}", cartId);

        } catch (OutOfStockException e) {
            log.warn("Order processing cancelled for cart ID: {}. Reason: {}", cartId, e.getMessage());
            // Handled in a separate REQUIRES_NEW transaction.
            cartErrorService.markCartAsCancelled(cartId);
            // ⬅️ Silently rolls back the current transaction without polluting the logs.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
            // Logs the collision; throwing it is mandatory to trigger the @Retryable mechanism.
            log.info("Concurrent processing conflict detected for cart ID: {}. Retrying...", cartId);
            throw e;
        } catch (BadRequestException e) {
            // Catches business validation errors to keep the console clean.
            log.warn("Invalid business data for cart ID: {}. Reason: {}", cartId, e.getMessage());
            cartErrorService.markCartAsFailed(cartId);
            // ⬅️ Silently rolls back the current transaction without polluting the logs.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Asynchronous processing interrupted for cart ID: {}", cartId, e);
            cartErrorService.markCartAsFailed(cartId);
        } catch (Exception e) {
            log.error("Unexpected error occurred while processing async order for cart ID: {}", cartId, e);
            cartErrorService.markCartAsFailed(cartId);
        }
    }

    /**
     * Fallback method triggered automatically by Spring Retry when all attempts are exhausted.
     * Prevents the cart from being stuck in a 'PROCESSING' state forever.
     */
    @SuppressWarnings("unused")
    @org.springframework.retry.annotation.Recover
    public void recoverFromConcurrencyFailure(ObjectOptimisticLockingFailureException e, OrderProcessingEvent event) {
        log.error("All retries exhausted due to massive concurrency for cart ID: {}. Marking as FAILED.", event.cartId());
        cartErrorService.markCartAsFailed(event.cartId());
    }

    /**
     * Fallback method triggered automatically by Spring Retry for StaleObjectStateException.
     */
    @SuppressWarnings("unused")
    @org.springframework.retry.annotation.Recover
    public void recoverFromStaleObject(StaleObjectStateException e, OrderProcessingEvent event) {
        log.error("All retries exhausted due to stale object for cart ID: {}. Marking as FAILED.", event.cartId());
        cartErrorService.markCartAsFailed(event.cartId());
    }
}