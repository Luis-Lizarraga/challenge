package com.lucho.tienda.service.impl;

import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartErrorServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartErrorService cartErrorService;

    @Test
    void markCartAsFailed_UpdatesStatusToFailed() {
        cartErrorService.markCartAsFailed(1L);
        verify(cartRepository).updateCartStatus(1L, CartStatus.FAILED);
    }

    @Test
    void markCartAsCancelled_UpdatesStatusToCancelled() {
        cartErrorService.markCartAsCancelled(1L);
        verify(cartRepository).updateCartStatus(1L, CartStatus.CANCELLED);
    }

    @Test
    void markCartAsFailed_HandlesExceptionGracefully() {
        doThrow(new RuntimeException("DB error")).when(cartRepository).updateCartStatus(1L, CartStatus.FAILED);

        // Debe capturar la excepción en el catch silencioso con log.error
        cartErrorService.markCartAsFailed(1L);

        verify(cartRepository).updateCartStatus(1L, CartStatus.FAILED);
    }
}