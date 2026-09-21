package com.lucho.tienda.constant;

public final class OpenApiMessageConstants {

    // Compact constructor to prevent instantiation
    private OpenApiMessageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // --- General Info ---
    public static final String TITLE = "E-Commerce Cart Management API";
    public static final String VERSION = "1.0.0";
    public static final String DESCRIPTION = "REST API for shopping cart management with asynchronous support and discounts.";
    public static final String SCHEME_NAME = "bearerAuth";
    public static final String SCHEME = "bearer";
    public static final String BEARER_FORMAT = "JWT";

    // --- Controller Tags & Operations ---
    public static final String TAG_AUTH_DESC = "Endpoint for login";
    public static final String TAG_CART_DESC = "Endpoints for cart management and order processing";
    public static final String OP_CREATE_CART = "Create a new cart";
    public static final String OP_CREATE_CART_DESC = "Creates a shopping cart associated with a user ID.";
    public static final String OP_ADD_PRODUCT = "Add a product to the cart";
    public static final String OP_UPDATE_PRODUCT_QTY = "Update product quantity in cart";
    public static final String OP_REMOVE_PRODUCT = "Remove a product from the cart";
    public static final String OP_GET_PRODUCTS = "Get cart products";
    public static final String OP_GET_USER_CARTS = "List carts by user";
    public static final String OP_PROCESS_ORDER = "Process order asynchronously";
    public static final String OP_GET_CART = "Get cart by id";

    // --- Success Messages ---
    public static final String CREATED = "Cart created successfully";
    public static final String OK_PRODUCT = "Product operation successful";
    public static final String OK_LIST = "List returned successfully";
    public static final String OK_CART = "Cart returned successfully";
    public static final String ACCEPTED = "Request accepted and being processed";

    // --- Error Messages ---
    public static final String BAD_REQUEST = "Invalid request parameters or validation failed";
    public static final String UNAUTHORIZED = "Unauthorized - Missing or invalid token";
    public static final String NOT_FOUND = "Resource not found (User, Cart, or Product)";
    public static final String CONFLICT = "Concurrency conflict or data integrity violation";
    public static final String ERROR = "Internal server error";
}