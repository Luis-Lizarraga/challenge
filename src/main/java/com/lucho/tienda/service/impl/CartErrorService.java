package com.lucho.tienda.service.impl;

import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartErrorService {

    private final CartRepository cartRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCartAsFailed(Long cartId) {
        try {
            cartRepository.updateCartStatus(cartId, CartStatus.FAILED);
            log.warn("Marked cart ID {} as FAILED due to processing error.", cartId);
        } catch (Exception ex) {
            log.error("Could not update cart ID {} status to FAILED", cartId, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCartAsCancelled(Long cartId) {
        try {
            cartRepository.updateCartStatus(cartId, CartStatus.CANCELLED);
            log.warn("Cart ID {} has been marked as CANCELLED due to out of stock.", cartId);
        } catch (Exception ex) {
            log.error("Could not update cart ID {} status to CANCELLED", cartId, ex);
        }
    }
}