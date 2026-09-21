package com.lucho.tienda.strategy;

import com.lucho.tienda.constant.CartConstants;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Discount;
import com.lucho.tienda.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DatabaseDiscountStrategy {

    private final DiscountRepository discountRepository;

    /**
     * 1. Pre-computes the active discounts into an O(1) Map.
     * This avoids nested loops when calculating discounts for multiple items.
     */
    public Map<Long, BigDecimal> getActiveDiscountsMap() {
        return discountRepository.findByActiveTrue().stream()
                .filter(Discount::getActive)
                .filter(d -> d.getCategory() != null && d.getCategory().getId() != null)
                .filter(d -> d.getPercentage() != null && d.getPercentage().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        d -> d.getCategory().getId(),
                        Discount::getPercentage,
                        (existing, replacement) -> existing // Keeps the first matching percentage if duplicates exist
                ));
    }

    /**
     * 2. Highly optimized method to calculate the discount for a single CartItem
     * using the pre-computed O(1) Map.
     */
    public BigDecimal calculateItemDiscount(CartItem item, Map<Long, BigDecimal> discountMap) {
        BigDecimal zeroScaled = BigDecimal.ZERO.setScale(CartConstants.MONEY_SCALE, RoundingMode.HALF_UP);

        if (item == null || item.getProduct() == null || item.getProduct().getCategory() == null) {
            return zeroScaled;
        }

        BigDecimal percentage = discountMap.getOrDefault(item.getProduct().getCategory().getId(), BigDecimal.ZERO);

        if (percentage.compareTo(BigDecimal.ZERO) == 0) {
            return zeroScaled;
        }

        BigDecimal basePrice = (item.getUnitPrice() != null) ? item.getUnitPrice() : item.getProduct().getPrice();

        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroScaled;
        }

        return basePrice.multiply(percentage)
                .divide(new BigDecimal(CartConstants.PERCENTAGE_DIVISOR), CartConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 3. Calculates the total discount for the entire cart.
     * It builds the map once and reuses it for all items.
     */
    public BigDecimal calculateDiscount(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<Long, BigDecimal> discountMap = getActiveDiscountsMap();

        if (discountMap.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cart.getItems().stream()
                .filter(Objects::nonNull)
                .map(item -> calculateItemDiscount(item, discountMap))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}