package com.lucho.tienda;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;

class TiendaApplicationTest {

    @Test
    void main_ShouldRunSpringApplication() {
        try (MockedStatic<SpringApplication> springAppMock = mockStatic(SpringApplication.class)) {

            assertDoesNotThrow(() -> TiendaApplication.main(new String[]{}));

            springAppMock.verify(() ->
                    SpringApplication.run(TiendaApplication.class, new String[]{})
            );
        }
    }
}