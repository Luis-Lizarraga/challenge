package com.lucho.tienda.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static com.lucho.tienda.constant.ApiEndpointConstants.*;
import static com.lucho.tienda.constant.ApiFieldConstants.*;
import static com.lucho.tienda.constant.ErrorMessageConstants.*;

class CustomAccessDeniedHandlerTest {

    private CustomAccessDeniedHandler accessDeniedHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // Configure a real ObjectMapper with support for LocalDateTime to prevent serialization errors
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void handle_ShouldReturn403AndErrorResponseJson() throws IOException {
        // Arrange: Simulate a request to a protected endpoint usando constantes
        String requestUri = FULL_ENDPOINT_CART_PRODUCTS.replace("{" + PARAM_CART_ID + "}", "1");
        request.setRequestURI(requestUri);
        AccessDeniedException ex = new AccessDeniedException("Simulated access denied");

        // Act: Execute the handler directly
        accessDeniedHandler.handle(request, response, ex);

        // Assert: Verify headers and HTTP status
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());

        // Fix: Verify it starts with application/json to account for the appended charset
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());

        // Assert: Verify the JSON response body
        String responseBody = response.getContentAsString();
        assertNotNull(responseBody, "The response body should not be null");

        // Verify that the JSON contains the correct values matching our ErrorResponse DTO
        assertTrue(responseBody.contains("\"status\":" + HttpStatus.FORBIDDEN.value()));
        assertTrue(responseBody.contains("\"error\":\"" + HttpStatus.FORBIDDEN.getReasonPhrase() + "\""));
        assertTrue(responseBody.contains("\"message\":\"" + ACCESS_DENIED_MESSAGE + "\""));
        assertTrue(responseBody.contains("\"path\":\"" + requestUri + "\""));

        // Verify that the timestamp was serialized (we only validate its presence, not the exact time)
        assertTrue(responseBody.contains("\"timestamp\""));
    }
}