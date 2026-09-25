package com.lucho.tienda.service.impl;

import com.lucho.tienda.constant.CartConstants;
import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.lucho.tienda.constant.ErrorMessageConstants.CART_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final CartRepository cartRepository;
    private final StockService stockService;
    private final OrderMetricsService orderMetricsService;

    /**
     * Processes a cart in its own transaction.
     *
     * The status check makes processing idempotent: if the same event is
     * delivered again after the cart has already been handled, it is ignored.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long cartId) {

        Cart cart = cartRepository.findCartById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CART_NOT_FOUND, cartId)
                ));

        if (cart.getStatus() != CartStatus.PROCESSING) {
            log.info(
                    "Order processing skipped for cart ID {}. Current status: {}",
                    cartId,
                    cart.getStatus()
            );
            return;
        }

        var timerSample = orderMetricsService.startTimer();

        try {
            stockService.deductStockForCart(cart);

            BigDecimal total = cart.getItems().stream()
                    .map(CartItem::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(
                            CartConstants.MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );

            cart.setTotalAmount(total);
            cart.markAsProcessed();

            // Count only checkouts that completed the business processing successfully.
            orderMetricsService.incrementProcessed();

            log.info(
                    "Order processed successfully for cart ID: {}",
                    cartId
            );
        } finally {
            // Record duration even when processing fails; this measures actual processing attempts.
            orderMetricsService.stopTimer(timerSample);
        }
    }
}