package com.lucho.tienda.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MessageConstantsTest {

    @Test
    void constructor_ThrowsException_WhenInstantiatedViaReflection() throws Exception {
        // Arrange
        Constructor<MessageConstants> constructor = MessageConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act & Assert
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                constructor::newInstance
        );

        // Verify the root cause inside InvocationTargetException
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("This is a utility class and cannot be instantiated", exception.getCause().getMessage());
    }
}