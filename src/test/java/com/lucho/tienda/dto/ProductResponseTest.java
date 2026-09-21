package com.lucho.tienda.dto;

import com.lucho.tienda.model.Category;
import com.lucho.tienda.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseTest {

    @Test
    void fromEntity_ReturnsValidResponse() {
        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setCode("PROD-01");
        product.setName("Whey Protein");
        product.setPrice(new BigDecimal("10500.00"));
        Category category = new Category();
        category.setName("PROTEINAS");
        product.setCategory(category);

        // Act
        ProductResponse response = ProductResponse.fromEntity(product);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("PROD-01", response.code());
        assertEquals("Whey Protein", response.name());
        assertEquals(new BigDecimal("10500.00"), response.price());
        assertEquals("PROTEINAS", response.category().name());
    }

    @Test
    void fromEntity_ReturnsNull_WhenProductIsNull() {
        // Act & Assert
        assertNull(ProductResponse.fromEntity(null));
    }

    @Test
    void fromEntityList_ReturnsValidList() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        Product product2 = new Product();
        product2.setId(2L);

        // Act
        List<ProductResponse> responses = ProductResponse.fromEntityList(List.of(product1, product2));

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals(2L, responses.get(1).id());
    }

    @Test
    void fromEntityList_ReturnsEmptyList_WhenInputIsNull() {
        // Act
        List<ProductResponse> responses = ProductResponse.fromEntityList(null);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
}