package com.lucho.tienda.constant;

public final class ErrorMessageConstants {

    // Compact constructor to prevent instantiation
    private ErrorMessageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // --- Domain Error Messages ---
    public static final String USER_ID_REQUIRED = "User ID is required";
    public static final String USER_NOT_FOUND = "User not found with ID: %d";
    public static final String QUANTITY_REQUIRED = "Quantity is required";
    public static final String MIN_QUANTITY_REQUIRED = "Quantity must be at least 1";

    public static final String CART_ID_REQUIRED = "Cart ID is required";
    public static final String CART_NOT_FOUND = "Cart not found with ID: %d";

    public static final String PRODUCT_CODE_REQUIRED = "Product code is required";
    public static final String PRODUCT_NOT_FOUND = "Product not found with code: %s";

    public static final String INVALID_ENUM_VALUE = "The value '%s' is not valid for the parameter '%s'";
    public static final String INVALID_TYPE_VALUE = "Invalid data type in the request parameter";

    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String PASSWORD_REQUIRED = "Password is required";
    // --- Global Exception Handler Error Messages ---
    public static final String VALIDATION_FAILED_PREFIX = "Validation failed: %s";
    public static final String AUTHENTICATION_FAILED_PREFIX = "Authentication failed: %s";
    public static final String ACCESS_DENIED_MESSAGE = "Access denied: You do not have permission to perform this action.";
    public static final String METHOD_NOT_SUPPORTED_TEMPLATE = "Request method '%s' is not supported for this endpoint.";
    public static final String CONCURRENCY_CONFLICT_MESSAGE = "The resource was modified by another transaction. Please try again.";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An internal server error occurred. Contact the administrator.";
    public static final String INVALID_OR_MISSING_TOKEN = "Invalid or missing authorization token";
    public static final String CART_ALREADY_PROCESSING = "The cart is currently being processed. Please wait.";
    public static final String CART_ALREADY_PROCESSED = "The cart has already been processed and cannot be modified.";
    public static final String CART_INVALID_STATE = "The cart is in an invalid state for checkout.";
    public static final String CART_FAILED_PROCESSING = "Cart processing failed and cannot be processed.";
    public static final String CART_CANCELLED_PROCESSING = "Cart checkout was cancelled due to lack of stock.";
    public static final String OUT_OF_STOCK = "Out of stock for product: %s";
    public static final String CANNOT_MODIFY_CART_STATUS = "Cannot modify cart in status: %s";

}