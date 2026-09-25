package com.lucho.tienda.service.impl;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartErrorServiceTest {

    private static final Long CART_ID = 1L;
    private static final String FAILURE_REASON = "Order processing failed";
    private static final String CANCELLATION_REASON = "Insufficient stock";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private Cart cart;

    @InjectMocks
    private CartErrorService cartErrorService;

    @Test
    void markAsFailed_MarksCartAsFailedWithReason() {
        when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));

        cartErrorService.markAsFailed(CART_ID, FAILURE_REASON);

        verify(cartRepository).findById(CART_ID);
        verify(cart).markAsFailed(FAILURE_REASON);
    }

    @Test
    void markAsCancelled_MarksCartAsCancelledWithReason() {
        when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart));

        cartErrorService.markAsCancelled(CART_ID, CANCELLATION_REASON);

        verify(cartRepository).findById(CART_ID);
        verify(cart).cancel(CANCELLATION_REASON);
    }
}
