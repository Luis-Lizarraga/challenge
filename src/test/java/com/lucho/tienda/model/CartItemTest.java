package com.lucho.tienda.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    // --- TESTS FOR equals() ---

    @Test
    void equals_SameInstance_ReturnsTrue() {
        CartItem item = new CartItem();
        assertEquals(item, item);
    }

    @Test
    void equals_NullOrDifferentClass_ReturnsFalse() {
        CartItem item = new CartItem();
        assertNotEquals(null, item);
        assertNotEquals("Not ACartItem", item);
    }

    @Test
    void equals_BothWithNonNullIds_ComparesById() {
        CartItem item1 = CartItem.builder().id(1L).build();
        CartItem item2 = CartItem.builder().id(1L).build();
        CartItem item3 = CartItem.builder().id(2L).build();

        assertEquals(item1, item2);
        assertNotEquals(item1, item3);
    }

    @Test
    void equals_NullIds_AreDifferentTransientEntities() {
        Product p1 = Product.builder().code("P01").build();
        Product p2 = Product.builder().code("P01").build();
        Product p3 = Product.builder().code("P02").build();

        CartItem item1 = CartItem.builder().product(p1).build();
        CartItem item2 = CartItem.builder().product(p2).build();
        CartItem item3 = CartItem.builder().product(p3).build();

        assertNotEquals(item1, item2);
        assertNotEquals(item1, item3);
    }

    @Test
    void equals_NullIds_ReturnsFalseWhenOneOrBothProductsNull() {
        Product p1 = Product.builder().code("P01").build();

        CartItem itemWithProduct = CartItem.builder().product(p1).build();
        CartItem itemNullProduct1 = CartItem.builder().product(null).build();
        CartItem itemNullProduct2 = CartItem.builder().product(null).build();

        assertNotEquals(itemWithProduct, itemNullProduct1);
        assertNotEquals(itemNullProduct1, itemWithProduct);
        assertNotEquals(itemNullProduct1, itemNullProduct2);
    }

    @Test
    void equals_OneIdNullAndOneIdNonNull_AreNotEqual() {
        Product p1 = Product.builder().code("P01").build();
        Product p2 = Product.builder().code("P01").build();

        CartItem itemWithId = CartItem.builder().id(1L).product(p1).build();
        CartItem itemWithoutId = CartItem.builder().id(null).product(p2).build();

        assertNotEquals(itemWithId, itemWithoutId);
    }

    // --- TESTS FOR hashCode() ---

    @Test
    void hashCode_ReturnsClassHashCode() {
        CartItem item1 = CartItem.builder().id(1L).build();
        CartItem item2 = CartItem.builder().id(2L).build();

        assertEquals(item1.hashCode(), item2.hashCode());
        assertEquals(CartItem.class.hashCode(), item1.hashCode());
    }

    // --- TESTS FOR getNetUnitPrice() ---

    @Test
    void getNetUnitPrice_ReturnsZero_WhenUnitPriceIsNull() {
        CartItem item = CartItem.builder()
                .unitPrice(null)
                .discountAmount(new BigDecimal("10.00"))
                .build();

        assertEquals(0, BigDecimal.ZERO.compareTo(item.getNetUnitPrice()));
    }

    @Test
    void getNetUnitPrice_SubtractsDiscount_WhenDiscountIsNotNull() {
        CartItem item = CartItem.builder()
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("20.00"))
                .build();

        // 100.00 - 20.00 = 80.00
        assertEquals(new BigDecimal("80.00"), item.getNetUnitPrice());
    }

    @Test
    void getNetUnitPrice_UsesZeroDiscount_WhenDiscountIsNull() {
        CartItem item = CartItem.builder()
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(null)
                .build();

        // 100.00 - 0.00 = 100.00
        assertEquals(new BigDecimal("100.00"), item.getNetUnitPrice());
    }

    // --- TESTS FOR getLineTotal() ---

    @Test
    void getLineTotal_MultipliesNetUnitPriceByQuantity_WhenQuantityIsNotNull() {
        CartItem item = CartItem.builder()
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("20.00"))
                .quantity(3)
                .build();

        // (100.00 - 20.00) * 3 = 240.00
        assertEquals(new BigDecimal("240.00"), item.getLineTotal());
    }

    @Test
    void getLineTotal_DefaultsQuantityToOne_WhenQuantityIsNull() {
        CartItem item = CartItem.builder()
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("20.00"))
                .quantity(null)
                .build();

        // (100.00 - 20.00) * 1 = 80.00
        assertEquals(new BigDecimal("80.00"), item.getLineTotal());
    }

    // --- TESTS FOR Lombok Getters, Setters, Builder, and Constructors ---

    @Test
    void lombok_GettersSettersBuilderAndConstructors_WorkCorrectly() {
        Cart cart = new Cart();
        Product product = new Product();
        BigDecimal unitPrice = new BigDecimal("50.00");
        BigDecimal discount = new BigDecimal("5.00");

        // Test NoArgsConstructor & Setters
        CartItem item = new CartItem();
        item.setId(10L);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(5);
        item.setUnitPrice(unitPrice);
        item.setDiscountAmount(discount);

        assertEquals(10L, item.getId());
        assertEquals(cart, item.getCart());
        assertEquals(product, item.getProduct());
        assertEquals(5, item.getQuantity());
        assertEquals(unitPrice, item.getUnitPrice());
        assertEquals(discount, item.getDiscountAmount());

        // Test AllArgsConstructor
        CartItem allArgsItem = new CartItem(20L, cart, product, 2, unitPrice, discount);
        assertEquals(20L, allArgsItem.getId());
        assertEquals(cart, allArgsItem.getCart());
        assertEquals(product, allArgsItem.getProduct());
        assertEquals(2, allArgsItem.getQuantity());

        // Test Builder
        CartItem builtItem = CartItem.builder()
                .id(30L)
                .cart(cart)
                .product(product)
                .quantity(4)
                .unitPrice(unitPrice)
                .discountAmount(discount)
                .build();

        assertEquals(30L, builtItem.getId());
        assertEquals(cart, builtItem.getCart());
        assertEquals(product, builtItem.getProduct());
        assertEquals(4, builtItem.getQuantity());
    }
}