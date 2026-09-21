package com.lucho.tienda.dto;

import com.lucho.tienda.constant.ErrorMessageConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductOperationRequest(
        @NotNull(message = ErrorMessageConstants.CART_ID_REQUIRED)
        Long cartId,

        @NotBlank(message = ErrorMessageConstants.PRODUCT_CODE_REQUIRED)
        String productCode,

        @NotNull(message = ErrorMessageConstants.QUANTITY_REQUIRED)
        @Min(value = 1, message = ErrorMessageConstants.MIN_QUANTITY_REQUIRED)
        Integer quantity
) {}