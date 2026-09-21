package com.lucho.tienda;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServletInitializerTest {

    @Mock
    private SpringApplicationBuilder builder;

    @Test
    void configure_ShouldAddTiendaApplicationSource() {
        ServletInitializer initializer = new ServletInitializer();
        when(builder.sources(TiendaApplication.class)).thenReturn(builder);

        SpringApplicationBuilder result = initializer.configure(builder);

        assertNotNull(result);
        verify(builder, times(1)).sources(TiendaApplication.class);
    }
}