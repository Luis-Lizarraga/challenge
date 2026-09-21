package com.lucho.tienda.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartTest {

    @Test
    void calculateTotal_CalculatesCorrectly_WithValidItems() {

        Cart cart = new Cart();

        CartItem item = new CartItem();
        item.setProduct(new Product());
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setQuantity(2);

        cart.addItem(item);

        BigDecimal total = cart.calculateTotal();

        assertBigDecimalEquals("200.00", total);
    }

    @Test
    void calculateTotal_HandlesNullPriceAndQuantity_Safely() {

        Cart cart = new Cart();

        CartItem item = new CartItem();
        item.setProduct(new Product());
        item.setUnitPrice(null);
        item.setQuantity(null);

        cart.addItem(item);

        BigDecimal total = cart.calculateTotal();

        assertBigDecimalEquals("0.00", total);
    }

    @Test
    void calculateTotal_ReturnsZero_WhenCartHasNoItems() {

        Cart cart = new Cart();

        BigDecimal total = cart.calculateTotal();

        assertBigDecimalEquals("0.00", total);
    }

    @Test
    void calculateTotal_IgnoresNullCartItems_Safely() {

        Cart cart = new Cart();

        CartItem validItem = new CartItem();
        validItem.setProduct(new Product());
        validItem.setUnitPrice(new BigDecimal("100.00"));
        validItem.setQuantity(1);

        cart.getItems().add(validItem);
        cart.getItems().add(null);

        BigDecimal total = cart.calculateTotal();

        assertBigDecimalEquals("100.00", total);
    }

    @Test
    void calculateTotal_IgnoresCartItemsWithNullProduct_Safely() {

        Cart cart = new Cart();

        CartItem item = new CartItem();
        item.setProduct(null);
        item.setUnitPrice(null);
        item.setQuantity(1);

        cart.addItem(item);

        BigDecimal total = cart.calculateTotal();

        assertBigDecimalEquals("0.00", total);
    }

    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual) {

        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual)
        );
    }
    @Test
    void startProcessing_ChangesCreatedCartToProcessing() {
        Cart cart = new Cart();
        CartItem item = new CartItem();
        item.setProduct(new Product());
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(1);
        cart.addItem(item);

        cart.startProcessing();

        assertEquals(com.lucho.tienda.model.enums.CartStatus.PROCESSING, cart.getStatus());
    }

    @Test
    void markAsProcessed_ChangesProcessingCartToProcessed() {
        Cart cart = new Cart();
        CartItem item = new CartItem();
        item.setProduct(new Product());
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(1);
        cart.addItem(item);
        cart.startProcessing();

        cart.markAsProcessed();

        assertEquals(com.lucho.tienda.model.enums.CartStatus.PROCESSED, cart.getStatus());
    }

    @Test
    void markAsProcessed_RejectsInvalidTransition() {
        Cart cart = new Cart();

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                cart::markAsProcessed
        );
    }

}