package com.lucho.tienda.service.impl;

import com.lucho.tienda.dto.ProductOperationRequest;
import com.lucho.tienda.event.OrderProcessingEvent;
import com.lucho.tienda.exception.BadRequestException;
import com.lucho.tienda.exception.ResourceNotFoundException;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.model.User;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.repository.CartRepository;
import com.lucho.tienda.repository.ProductRepository;
import com.lucho.tienda.repository.UserRepository;
import com.lucho.tienda.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.lucho.tienda.constant.ErrorMessageConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Cart createCart(Long userId) {
        if (userId == null) {
            throw new BadRequestException(USER_ID_REQUIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(USER_NOT_FOUND, userId)));

        Cart cart = Cart.builder()
                .user(user)
                .build();
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart addProduct(Long userId, ProductOperationRequest request) {
        Cart cart = cartRepository.findByIdAndUserIdForUpdate(request.cartId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CART_NOT_FOUND, request.cartId())));

        validateCartModifiable(cart);
        Product product = getProduct(request.productCode());

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getCode().equalsIgnoreCase(product.getCode()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            updateExistingItemQuantity(existingItemOpt.get(), product, request.quantity());
        } else {
            createNewCartItem(cart, product, request.quantity());
        }

        return saveCartWithUpdatedTotal(cart);
    }

    @Override
    @Transactional
    public Cart updateProductQuantity(Long userId, ProductOperationRequest request) {
        if (request.quantity() == null || request.quantity() < 1) {
            throw new BadRequestException(MIN_QUANTITY_REQUIRED);
        }

        Cart cart = cartRepository.findByIdAndUserIdForUpdate(request.cartId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CART_NOT_FOUND, request.cartId())));
        validateCartModifiable(cart);

        CartItem itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getProduct().getCode().equalsIgnoreCase(request.productCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PRODUCT_NOT_FOUND, request.productCode())));

        Product product = itemToUpdate.getProduct();
        if (product.getStock() < request.quantity()) {
            throw new BadRequestException(String.format(OUT_OF_STOCK, product.getCode()));
        }

        itemToUpdate.changeQuantity(request.quantity());
        return saveCartWithUpdatedTotal(cart);
    }

    @Override
    @Transactional
    public Cart removeProduct(Long userId, Long cartId, String productCode) {
        Cart cart = cartRepository.findByIdAndUserIdForUpdate(cartId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CART_NOT_FOUND, cartId)));
        validateCartModifiable(cart);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProduct().getCode().equalsIgnoreCase(productCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PRODUCT_NOT_FOUND, productCode)));

        cart.removeItem(itemToRemove);
        return saveCartWithUpdatedTotal(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartProducts(Long userId, Long cartId) {
        if (cartId == null) {
            throw new BadRequestException(CART_ID_REQUIRED);
        }

        Cart cart = getCartForUser(userId, cartId);
        return cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cart> getUserCarts(Long userId, CartStatus status) {
        if (userId == null) {
            throw new BadRequestException(USER_ID_REQUIRED);
        }
        return status != null
                ? cartRepository.findByUserIdAndStatus(userId, status)
                : cartRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void initiateCheckout(Long userId, Long cartId) {
        Cart cart = cartRepository.findByIdAndUserIdForUpdate(cartId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CART_NOT_FOUND, cartId)));

        cart.startProcessing();
        eventPublisher.publishEvent(new OrderProcessingEvent(cart.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Cart getCartById(Long userId, Long cartId) {
        if (cartId == null) {
            throw new BadRequestException(CART_ID_REQUIRED);
        }
        return getCartForUser(userId, cartId);
    }

    private Cart getCartForUser(Long userId, Long cartId) {
        if (userId == null) {
            throw new BadRequestException(USER_ID_REQUIRED);
        }
        return cartRepository.findByIdAndUserId(cartId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CART_NOT_FOUND, cartId)));
    }

    private Product getProduct(String code) {
        return productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PRODUCT_NOT_FOUND, code)));
    }

    /**
     * Validates that the cart is in a state that allows modifications.
     */
    private void validateCartModifiable(Cart cart) {
        if (cart.getStatus().cannotModify()) {
            throw new BadRequestException(String.format(CANNOT_MODIFY_CART_STATUS, cart.getStatus()));
        }
    }

    /**
     * Recalculates the cart total (triggering @Version increment) and saves it.
     */
    private Cart saveCartWithUpdatedTotal(Cart cart) {
        cart.recalculateTotal();
        return cart;
    }
    /**
     * Helper method to update the quantity of an existing item in the cart.
     * Crucially re-validates stock using the reloaded product data to maintain integrity (Senior Best Practice).
     * This is vital in concurrent environments to ensure the final quantity does not exceed reloaded stock.
     */
    private void updateExistingItemQuantity(CartItem item, Product product, Integer additionalQuantity) {
        // Recalculate the final quantity by adding the new additional quantity.
        int finalQuantity = item.getQuantity() + additionalQuantity;

        // Perform strict stock re-validation using the reloaded product data to maintain integrity (Senior Best Practice).
        // The initial check against obsolete memory data might have been misleading in a concurrent race.
        if (product.getStock() < finalQuantity) {
            throw new BadRequestException(String.format(OUT_OF_STOCK, product.getCode()));
        }

        // Set the final validated quantity on the item.
        item.changeQuantity(finalQuantity);
    }

    /**
     * Helper method to create a new item and add it to the cart.
     * Validates initial stock preventatively and handles the bidirectional association properly.
     */
    private void createNewCartItem(Cart cart, Product product, Integer requestedQuantity) {
        // Validate initial stock preventatively for the new item.
        if (product.getStock() < requestedQuantity) {
            throw new BadRequestException(String.format(OUT_OF_STOCK, product.getCode()));
        }

        // Create the new item using the Builder pattern (established practice).
        // Notice we explicitly set the parent Cart reference here.
        CartItem newItem = CartItem.builder()
                .product(product)
                .quantity(requestedQuantity)
                .unitPrice(product.getPrice()) // Assume we set this from the product at this moment.
                .discountAmount(BigDecimal.ZERO) // Assume a default or no discount.
                .build();

        // Add the new item to the cart using the helper method established in the parent entity,
        // which should handle the bidirectional relationship consistency.
        cart.addItem(newItem);
    }
}