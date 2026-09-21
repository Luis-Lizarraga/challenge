package com.lucho.tienda.service;

import com.lucho.tienda.dto.ProductOperationRequest;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.enums.CartStatus;

import java.util.List;

public interface CartService {

    Cart createCart(Long userId);

    Cart addProduct(Long userId, ProductOperationRequest request);

    Cart updateProductQuantity(Long userId, ProductOperationRequest request);

    Cart removeProduct(Long userId, Long cartId, String productCode);

    List<CartItem> getCartProducts(Long userId, Long cartId);

    List<Cart> getUserCarts(Long userId, CartStatus status);

    Cart getCartById(Long userId, Long cartId);

    void initiateCheckout(Long userId, Long cartId);
}
