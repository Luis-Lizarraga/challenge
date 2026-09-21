package com.lucho.tienda.service.impl;

import com.lucho.tienda.exception.OutOfStockException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.repository.ProductRepository;
import com.lucho.tienda.strategy.DatabaseDiscountStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final DatabaseDiscountStrategy discountStrategy;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deductStockForCart(Cart cart) {
        // Defensa silenciosa: Si por la concurrencia llega vacío, simplemente retornamos sin romper el hilo
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> activeDiscountsMap = discountStrategy.getActiveDiscountsMap();

        for (CartItem item : cart.getItems()) {
            if (item == null || item.getProduct() == null) {
                continue;
            }

            Product product = item.getProduct();
            int requiredQuantity = (item.getQuantity() != null && item.getQuantity() > 0)
                    ? item.getQuantity()
                    : 1;

            int updatedRows = productRepository.decrementStockSafely(product.getCode(), requiredQuantity);

            if (updatedRows == 0) {
                throw new OutOfStockException(product.getCode());
            }

            BigDecimal unitPrice = product.getPrice();
            item.setUnitPrice(unitPrice);

            BigDecimal discountPerUnit = discountStrategy.calculateItemDiscount(item, activeDiscountsMap);
            item.setDiscountAmount(discountPerUnit);
        }
    }
}