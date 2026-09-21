package com.lucho.tienda.model.enums;

import com.lucho.tienda.constant.ErrorMessageConstants;
import com.lucho.tienda.exception.BadRequestException;

public enum CartStatus {
    CREATED,
    PROCESSING,
    PROCESSED,
    CANCELLED,
    FAILED;

    /**
     * Validates if the current state allows starting the checkout process.
     * Throws an exception if the state is invalid.
     */
    public void validateForCheckout() {
        switch (this) {
            case PROCESSING ->
                    throw new BadRequestException(ErrorMessageConstants.CART_ALREADY_PROCESSING);
            case PROCESSED ->
                    throw new BadRequestException(ErrorMessageConstants.CART_ALREADY_PROCESSED);
            case FAILED ->
                    throw new BadRequestException(ErrorMessageConstants.CART_FAILED_PROCESSING);
            case CANCELLED ->
                    throw new BadRequestException(ErrorMessageConstants.CART_CANCELLED_PROCESSING);
            case CREATED -> {
                // Do nothing, this is the only valid state for checkout
            }
            default ->
                    throw new BadRequestException(ErrorMessageConstants.CART_INVALID_STATE);
        }
    }

    /**
     * Indicates whether the cart cannot be modified in its current status.
     */
    public boolean cannotModify() {
        return switch (this) {
            case CREATED -> false;
            default -> true;
        };
    }
}