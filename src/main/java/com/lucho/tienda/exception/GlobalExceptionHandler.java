package com.lucho.tienda.exception;

import com.lucho.tienda.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static com.lucho.tienda.constant.ErrorMessageConstants.*;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. HTTP 400 - Handles invalid requests and domain business rule violations
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad Request (400): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // 2. HTTP 400 - Handles DTO field validation failures (@Valid on @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detailMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("'%s': %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        String formattedMessage = String.format(VALIDATION_FAILED_PREFIX, detailMessage);
        log.warn("Validation Failed (400): {}", formattedMessage);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, formattedMessage, request.getRequestURI());
    }

    // 3. HTTP 401 - Handles JWT authentication failures and invalid credentials
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Unauthorized (401): {}", ex.getMessage());
        String formattedMessage = String.format(AUTHENTICATION_FAILED_PREFIX, ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, formattedMessage, request.getRequestURI());
    }

    // 4. HTTP 403 - Handles access denied exceptions when user lacks required roles/permissions
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ACCESS_DENIED_MESSAGE, request.getRequestURI());
    }

    // 5. HTTP 404 - Handles missing entities (Cart, User, or Product not found)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource Not Found (404): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // 6. HTTP 405 - Handles unsupported HTTP methods (e.g., sending a POST to a DELETE route)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method Not Allowed (405): {}", ex.getMessage());
        String formattedMessage = String.format(METHOD_NOT_SUPPORTED_TEMPLATE, ex.getMethod());
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, formattedMessage, request.getRequestURI());
    }

    // 7. HTTP 409 - Handles concurrency conflicts driven by JPA @Version optimistic locking
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Concurrency Conflict (409): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, CONCURRENCY_CONFLICT_MESSAGE, request.getRequestURI());
    }

    // 8. HTTP 500 - Fallback handler for unhandled internal server errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Internal Server Error (500): ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE, request.getRequestURI());
    }
    // 9. HTTP 409 (Conflict) o 422 (Unprocessable Entity) - Falta de stock
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException ex, HttpServletRequest request) {
        log.warn("Out of Stock (409): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleCustomUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized API Access (401): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String paramName = ex.getName();
        String providedValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String message;

        // Check if the error comes from an Enum to provide a more precise message
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = String.format(INVALID_ENUM_VALUE, providedValue, paramName);
        } else {
            message = INVALID_TYPE_VALUE;
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    // Generic handler for any business rule exception not caught individually
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business Exception (400): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // --- Helper Utility Method ---
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(error);
    }
}