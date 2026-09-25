package com.lucho.tienda.service;

import com.lucho.tienda.dto.CartItemResponse;
import com.lucho.tienda.dto.CartResponse;
import com.lucho.tienda.dto.ProductOperationRequest;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.enums.CartStatus;

import java.util.List;

public interface CartService {

    CartResponse createCart(Long userId);

    CartResponse addProduct(Long userId, ProductOperationRequest request);

    CartResponse updateProductQuantity(Long userId, ProductOperationRequest request);

    CartResponse removeProduct(Long userId, Long cartId, String productCode);

    List<CartItemResponse> getCartProducts(Long userId, Long cartId);

    List<CartResponse> getUserCarts(Long userId, CartStatus status);

    CartResponse getCartById(Long userId, Long cartId);

    void initiateCheckout(Long userId, Long cartId);

    Cart getCheckoutStatus(Long userId, Long cartId);
}
