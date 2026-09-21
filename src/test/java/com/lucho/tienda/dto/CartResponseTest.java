package com.lucho.tienda.dto;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.model.User;
import com.lucho.tienda.model.enums.CartStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CartResponseTest {

    @Test
    void fromEntity_ReturnsValidResponse() {
        // Arrange
        User user = new User();
        user.setId(99L);

        Product product = new Product();
        product.setId(1L);
        product.setCode("PROD-01");
        product.setName("Whey Protein");
        product.setPrice(new BigDecimal("250.00"));

        Cart cart = new Cart();
        cart.setId(100L);
        cart.setStatus(CartStatus.PROCESSED);
        cart.setTotalAmount(new BigDecimal("250.00"));
        cart.setUser(user);
        cart.setItems(new HashSet<>() {
        });

        CartItem cartItem = new CartItem();
        cartItem.setId(10L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        cart.getItems().add(cartItem);

        // Act
        CartResponse response = CartResponse.fromEntity(cart);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(CartStatus.PROCESSED, response.status());
        assertEquals(new BigDecimal("250.00"), response.totalAmount());

        assertNotNull(response.items());
        assertEquals(1, response.items().size());
        assertEquals("PROD-01", response.items().getFirst().product().code());
        assertEquals(2, response.items().getFirst().quantity());
    }

    @Test
    void fromEntity_HandlesNullUserSafely() {
        // Arrange
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(null);
        cart.setItems(new HashSet<>() {
        });
        // Act
        CartResponse response = CartResponse.fromEntity(cart);

        // Assert
        assertNotNull(response);
        assertTrue(response.items().isEmpty());
    }

    @Test
    void fromEntity_HandlesNullItemsSafely() {
        // Arrange
        Cart cart = new Cart();
        cart.setId(15L);
        // Validate that it doesn't crash if the list is null
        cart.setItems(null);

        // Act
        CartResponse response = CartResponse.fromEntity(cart);

        // Assert
        assertNotNull(response);
        assertTrue(response.items().isEmpty());
    }

    @Test
    void fromEntity_ReturnsNull_WhenCartIsNull() {
        // Act & Assert
        assertNull(CartResponse.fromEntity(null));
    }

    @Test
    void fromEntityList_ReturnsValidList() {
        // Arrange
        Cart cart1 = new Cart();
        cart1.setId(1L);
        cart1.setItems(new HashSet<>() {
        });
        Cart cart2 = new Cart();
        cart2.setId(2L);
        cart2.setItems(new HashSet<>() {
        });
        // Act
        List<CartResponse> responses = CartResponse.fromEntityList(List.of(cart1, cart2));

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals(2L, responses.get(1).id());
    }

    @Test
    void fromEntityList_ReturnsEmptyList_WhenInputIsNull() {
        // Act
        List<CartResponse> responses = CartResponse.fromEntityList(null);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void fromEntityList_KeepsNullElements_Safely() {
        // Arrange
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setItems(new HashSet<>() {
        });

        List<Cart> inputList = new ArrayList<>();
        inputList.add(cart);
        inputList.add(null); // Inject a null element to test branch coverage

        // Act
        List<CartResponse> responses = CartResponse.fromEntityList(inputList);

        // Assert
        assertNotNull(responses);
        // The implementation maps [cart, null] to [mappedCart, null], so size is 2
        assertEquals(2, responses.size());
        assertNull(responses.get(1)); // Verify the null was mapped as null
    }

    @Test
    void fromEntityList_ReturnsEmptyList_WhenInputIsEmpty() {
        // Arrange: An empty list (not null, just empty)
        List<Cart> emptyList = new ArrayList<>();

        // Act
        List<CartResponse> responses = CartResponse.fromEntityList(emptyList);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
}