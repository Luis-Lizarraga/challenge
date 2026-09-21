package com.lucho.tienda.model.enums;

import com.lucho.tienda.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartStatusTest {

    // --- CUSTOM BUSINESS LOGIC TESTS ---

    @Test
    void validateForCheckout_DoesNotThrow_WhenStatusIsCreated() {
        // Arrange
        CartStatus status = CartStatus.CREATED;

        // Act & Assert: CREATED is the only valid state, so it should not throw
        assertDoesNotThrow(status::validateForCheckout);
    }

    @Test
    void validateForCheckout_ThrowsBadRequest_WhenStatusIsProcessing() {
        // Arrange
        CartStatus status = CartStatus.PROCESSING;

        // Act & Assert
        assertThrows(BadRequestException.class, status::validateForCheckout);
    }

    @Test
    void validateForCheckout_ThrowsBadRequest_WhenStatusIsProcessed() {
        // Arrange
        CartStatus status = CartStatus.PROCESSED;

        // Act & Assert
        assertThrows(BadRequestException.class, status::validateForCheckout);
    }

    @Test
    void validateForCheckout_ThrowsBadRequest_WhenStatusIsCancelled() {
        // Arrange
        CartStatus status = CartStatus.CANCELLED;

        // Act & Assert
        assertThrows(BadRequestException.class, status::validateForCheckout);
    }

    // --- STANDARD ENUM METHODS COVERAGE ---

    @Test
    void enumValues_AreCorrectlyDefined_ForFullCoverage() {
        // Act
        CartStatus[] statuses = CartStatus.values();

        // Assert: Invoking values() and valueOf() is required for 100% enum coverage
        assertEquals(5, statuses.length);
        assertEquals(CartStatus.CREATED, CartStatus.valueOf("CREATED"));
        assertEquals(CartStatus.PROCESSING, CartStatus.valueOf("PROCESSING"));
        assertEquals(CartStatus.PROCESSED, CartStatus.valueOf("PROCESSED"));
        assertEquals(CartStatus.CANCELLED, CartStatus.valueOf("CANCELLED"));
    }
}