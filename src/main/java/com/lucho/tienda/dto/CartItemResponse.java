package com.lucho.tienda.dto;

import com.lucho.tienda.model.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(
        Long id,
        ProductResponse product,
        Integer quantity,
        BigDecimal unitPrice,        // Original price captured at checkout
        BigDecimal discountAmount,   // Frozen unit discount captured at checkout
        BigDecimal netUnitPrice,     // Real unit price paid (unitPrice - discountAmount)
        BigDecimal lineTotal         // Final row total (netUnitPrice * quantity)
) {
    public static CartItemResponse fromEntity(CartItem item) {
        if (item == null) return null;

        BigDecimal listPrice = (item.getUnitPrice() != null)
                ? item.getUnitPrice()
                : item.getProduct().getPrice();

        return new CartItemResponse(
                item.getId(),
                ProductResponse.fromEntity(item.getProduct()),
                item.getQuantity() != null ? item.getQuantity() : 1,
                listPrice,
                item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO,
                item.getNetUnitPrice(),
                item.getLineTotal()
        );
    }

    public static List<CartItemResponse> fromEntityList(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return items.stream()
                .map(CartItemResponse::fromEntity)
                .toList();
    }
}