package com.lucho.tienda.strategy;

import com.lucho.tienda.model.Cart;
import java.math.BigDecimal;

public interface DiscountStrategy {
    BigDecimal calculateDiscount(Cart cart);
}