package com.lucho.tienda.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void businessException_ShouldHoldMessageAndBeRuntimeException() {
        String errorMessage = "Business rule error";
        BusinessException exception = new BusinessException(errorMessage);

        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}