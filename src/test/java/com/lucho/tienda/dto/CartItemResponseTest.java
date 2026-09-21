package com.lucho.tienda.dto;

import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Category;
import com.lucho.tienda.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartItemResponseTest {

    // Helper to compare BigDecimals ignoring scale and avoiding compareTo confusion
    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        if (expected == null) {
            assertNull(actual, "Expected null but was " + actual);
        } else {
            assertNotNull(actual, "Expected " + expected + " but was null");
            assertEquals(new BigDecimal(expected).stripTrailingZeros(), actual.stripTrailingZeros());
        }
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertNotNull(actual);
            assertEquals(expected.stripTrailingZeros(), actual.stripTrailingZeros());
        }
    }

    // --- TESTS FOR fromEntity(CartItem item) ---

    @Test
    void fromEntity_ReturnsNull_WhenItemIsNull() {
        assertNull(CartItemResponse.fromEntity(null));
    }

    @Test
    void fromEntity_ReturnsValidResponse_WithFrozenUnitPriceAndDiscountAndPositiveQuantity() {
        Product product = new Product();
        product.setId(5L);
        product.setCode("P-100");
        product.setName("Creatine");
        product.setCategory(new Category());
        product.setPrice(new BigDecimal("150.00"));

        CartItem item = new CartItem();
        item.setId(1L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("200.00"));
        item.setDiscountAmount(new BigDecimal("30.00"));
        item.setProduct(product);

        CartItemResponse response = CartItemResponse.fromEntity(item);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("P-100", response.product().code());
        assertEquals(2, response.quantity());

        assertBigDecimalEquals("200.00", response.unitPrice());
        assertBigDecimalEquals("30.00", response.discountAmount());
        assertBigDecimalEquals("170.00", response.netUnitPrice()); // 200 - 30
        assertBigDecimalEquals("340.00", response.lineTotal());    // 170 * 2
    }

    @Test
    void fromEntity_FallsBackToCatalogPriceAndZeroDiscount_WhenFrozenValuesAreNull() {
        Product product = new Product();
        product.setId(5L);
        product.setCode("P-200");
        product.setPrice(new BigDecimal("150.00"));

        CartItem item = new CartItem();
        item.setId(2L);
        item.setQuantity(3);
        item.setUnitPrice(null);
        item.setDiscountAmount(null);
        item.setProduct(product);

        CartItemResponse response = CartItemResponse.fromEntity(item);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("P-200", response.product().code());
        assertEquals(3, response.quantity());

        // Verified: The DTO correctly falls back to the product's catalog price
        assertBigDecimalEquals("150.00", response.unitPrice());

        // Verified: The DTO maps a missing discount to 0
        assertBigDecimalEquals("0", response.discountAmount());

        // Verified: Since CartItem.getNetUnitPrice() and getLineTotal() return 0
        // when unitPrice is null, the DTO maps those zeroes directly.
        assertBigDecimalEquals("0", response.netUnitPrice());
        assertBigDecimalEquals("0", response.lineTotal());
    }

    @Test
    void fromEntity_DefaultsQuantityToOne_WhenQuantityIsNull() {
        Product product = new Product();
        product.setCode("P-300");
        product.setPrice(new BigDecimal("100.00"));

        CartItem item = new CartItem();
        item.setId(3L);
        item.setQuantity(null);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setDiscountAmount(BigDecimal.ZERO);
        item.setProduct(product);

        CartItemResponse response = CartItemResponse.fromEntity(item);

        assertNotNull(response);
        // Verified: The DTO successfully converts null quantities to 1
        assertEquals(1, response.quantity());
        assertBigDecimalEquals("100.00", response.lineTotal());
    }

    @Test
    void fromEntity_MapsQuantityExactly_WhenQuantityIsZeroOrNegative() {
        Product product = new Product();
        product.setCode("P-400");
        product.setPrice(new BigDecimal("100.00"));

        // Testing quantity = 0
        CartItem itemZeroQty = new CartItem();
        itemZeroQty.setId(4L);
        itemZeroQty.setQuantity(0);
        itemZeroQty.setProduct(product);

        CartItemResponse responseZero = CartItemResponse.fromEntity(itemZeroQty);
        assertNotNull(responseZero);
        // Verified: The DTO maps 0 exactly as 0
        assertEquals(0, responseZero.quantity());

        // Testing negative quantity
        CartItem itemNegativeQty = new CartItem();
        itemNegativeQty.setId(5L);
        itemNegativeQty.setQuantity(-5);
        itemNegativeQty.setProduct(product);

        CartItemResponse responseNegative = CartItemResponse.fromEntity(itemNegativeQty);
        assertNotNull(responseNegative);
        // Verified: The DTO maps negative quantities exactly as they are
        assertEquals(-5, responseNegative.quantity());
    }

    @Test
    void fromEntity_ThrowsNPE_WhenProductIsNull() {
        CartItem item = new CartItem();
        item.setId(6L);
        item.setQuantity(1);
        item.setProduct(null);

        assertThrows(NullPointerException.class, () -> CartItemResponse.fromEntity(item));
    }

    @Test
    void fromEntity_HandlesNullProductFields_Safely() {
        Product product = new Product();
        product.setId(7L);
        product.setCode(null);
        product.setPrice(BigDecimal.TEN);

        CartItem item = new CartItem();
        item.setId(1L);
        item.setQuantity(2);
        item.setProduct(product);

        CartItemResponse response = CartItemResponse.fromEntity(item);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertNull(response.product().code());
        assertEquals(2, response.quantity());
    }

    // --- TESTS FOR fromEntityList(List<CartItem> items) ---

    @Test
    void fromEntityList_ReturnsEmptyList_WhenListIsNull() {
        List<CartItemResponse> responses = CartItemResponse.fromEntityList(null);
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void fromEntityList_ReturnsEmptyList_WhenListIsEmpty() {
        List<CartItemResponse> responses = CartItemResponse.fromEntityList(List.of());
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void fromEntityList_ReturnsMappedList_WhenListHasValidItems() {
        Product p1 = new Product(); p1.setCode("P-1"); p1.setPrice(BigDecimal.TEN);
        CartItem item1 = new CartItem(); item1.setId(10L); item1.setProduct(p1);

        Product p2 = new Product(); p2.setCode("P-2"); p2.setPrice(BigDecimal.ONE);
        CartItem item2 = new CartItem(); item2.setId(20L); item2.setProduct(p2);

        List<CartItemResponse> responses = CartItemResponse.fromEntityList(List.of(item1, item2));

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(10L, responses.get(0).id());
        assertEquals("P-1", responses.get(0).product().code());
        assertEquals(20L, responses.get(1).id());
        assertEquals("P-2", responses.get(1).product().code());
    }

    @Test
    void fromEntityList_HandlesListWithNullElements_Safely() {
        Product p1 = new Product(); p1.setCode("P-1"); p1.setPrice(BigDecimal.TEN);
        CartItem item1 = new CartItem(); item1.setId(10L); item1.setProduct(p1);

        List<CartItem> itemsWithNull = new ArrayList<>();
        itemsWithNull.add(item1);
        itemsWithNull.add(null);

        List<CartItemResponse> responses = CartItemResponse.fromEntityList(itemsWithNull);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertNotNull(responses.get(0));
        assertNull(responses.get(1));
    }
}