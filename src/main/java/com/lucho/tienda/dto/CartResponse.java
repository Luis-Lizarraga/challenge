package com.lucho.tienda.dto;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.enums.CartStatus;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        CartStatus status,
        BigDecimal totalAmount,
        List<CartItemResponse> items
) {
    public static CartResponse fromEntity(Cart cart) {
        if (cart == null) return null;

        List<CartItemResponse> itemResponses = (cart.getItems() != null)
                ? cart.getItems().stream().map(CartItemResponse::fromEntity).toList()
                : List.of();

        return new CartResponse(
                cart.getId(),
                cart.getUser() != null ? cart.getUser().getId() : null,
                cart.getStatus(),
                cart.getTotalAmount(),
                itemResponses
        );
    }

    public static List<CartResponse> fromEntityList(List<Cart> carts) {
        if (carts == null || carts.isEmpty()) {
            return List.of();
        }
        return carts.stream()
                .map(CartResponse::fromEntity)
                .toList();
    }
}