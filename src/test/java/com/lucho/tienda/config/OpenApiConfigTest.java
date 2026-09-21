package com.lucho.tienda.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void customOpenAPI_BuildsOpenAPIInstanceCorrectly() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("E-Commerce Cart Management API", openAPI.getInfo().getTitle());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("bearerAuth"));
    }
}