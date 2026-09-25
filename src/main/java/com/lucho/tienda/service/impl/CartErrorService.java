package com.lucho.tienda.service.impl;

import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.lucho.tienda.constant.ErrorMessageConstants.CART_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartErrorService {

    private final CartRepository cartRepository;

    /**
     * Persists a failed processing result in an independent transaction.
     * This allows the failure state to survive even when the caller transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long cartId, String reason) {
        Cart cart = getCart(cartId);
        cart.markAsFailed(reason);
        log.warn("Cart ID {} marked as FAILED. Reason: {}", cartId, reason);
    }

    /**
     * Persists a cancellation in an independent transaction, including the reason
     * that caused order processing to stop.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCancelled(Long cartId, String reason) {
        Cart cart = getCart(cartId);
        cart.cancel(reason);
        log.warn("Cart ID {} marked as CANCELLED. Reason: {}", cartId, reason);
    }

    private Cart getCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CART_NOT_FOUND, cartId)));
    }
}
