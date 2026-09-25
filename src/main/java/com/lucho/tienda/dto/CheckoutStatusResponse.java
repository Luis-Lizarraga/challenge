package com.lucho.tienda.dto;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.enums.CartStatus;
import static com.lucho.tienda.constant.MessageConstants.*;

public record CheckoutStatusResponse(
        Long cartId,
        CartStatus status,
        String message,
        String failureReason
) {

    public static CheckoutStatusResponse fromEntity(Cart cart) {
        return new CheckoutStatusResponse(
                cart.getId(),
                cart.getStatus(),
                resolveMessage(cart.getStatus()),
                cart.getFailureReason()
        );
    }

    private static String resolveMessage(CartStatus status) {
        return switch (status) {
            case CREATED -> CHECKOUT_NOT_STARTED;
            case PROCESSING -> PROCESSING_ORDER;
            case PROCESSED -> ORDER_PROCESSED;
            case FAILED -> ORDER_PROCESSING_FAILED;
            case CANCELLED -> ORDER_CANCELLED;
        };
    }
}