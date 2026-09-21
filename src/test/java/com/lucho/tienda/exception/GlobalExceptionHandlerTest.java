package com.lucho.tienda.exception;

import com.lucho.tienda.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.lucho.tienda.constant.ErrorMessageConstants.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    void handleBadRequest_Returns400() {
        BadRequestException ex = new BadRequestException("Invalid payload");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.error());
        assertEquals("Invalid payload", body.message());
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleValidationErrors_Returns400() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("createCartRequest", "userId", "must not be null");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        org.springframework.core.MethodParameter parameter = mock(org.springframework.core.MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.error());
        assertTrue(body.message().contains("'userId': must not be null"));
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleAuthenticationException_Returns401() {
        AuthenticationException ex = mock(AuthenticationException.class);
        when(ex.getMessage()).thenReturn("Bad credentials");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(401, body.status());
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.error());
        assertEquals("Authentication failed: Bad credentials", body.message());
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleAccessDenied_Returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.status());
        assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), body.error());
        assertEquals(ACCESS_DENIED_MESSAGE, body.message());
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleResourceNotFound_Returns404() {
        String notFoundMessage = String.format(CART_NOT_FOUND, 10L);
        ResourceNotFoundException ex = new ResourceNotFoundException(notFoundMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.status());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), body.error());
        assertEquals(notFoundMessage, body.message());
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleMethodNotSupported_Returns405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(405, body.status());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(), body.error());
        assertEquals(String.format(METHOD_NOT_SUPPORTED_TEMPLATE, "POST"), body.message());
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleOptimisticLocking_Returns409() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException("Cart", 1L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOptimisticLocking(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.status());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), body.error());
        assertEquals(CONCURRENCY_CONFLICT_MESSAGE, body.message());
        assertEquals("/api/test", body.path());
    }

    @Test
    void handleGeneric_Returns500() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneric(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), body.error());
        assertEquals(INTERNAL_SERVER_ERROR_MESSAGE, body.message());
        assertEquals("/api/test", body.path());
    }
}