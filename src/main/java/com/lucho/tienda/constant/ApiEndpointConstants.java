package com.lucho.tienda.constant;

public final class ApiEndpointConstants {

    // Compact constructor to prevent instantiation
    private ApiEndpointConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String API_BASE = "/api";

    // --- AUTH API ---
    public static final String ENDPOINT_AUTH = API_BASE + "/auth";
    public static final String SUB_ENDPOINT_LOGIN = "/login";

    // --- CARTS API ---
    public static final String ENDPOINT_CARTS = API_BASE + "/carts";

    public static final String SUB_ENDPOINT_CART_PRODUCTS = "/{" + ApiFieldConstants.PARAM_CART_ID + "}/products";
    public static final String SUB_ENDPOINT_REMOVE_PRODUCT = SUB_ENDPOINT_CART_PRODUCTS + "/{" + ApiFieldConstants.FIELD_PRODUCT_CODE + "}";
    public static final String SUB_ENDPOINT_PROCESS_ORDER = "/{" + ApiFieldConstants.PARAM_CART_ID + "}/process";
    public static final String SUB_ENDPOINT_GET_CART = "/{" + ApiFieldConstants.PARAM_CART_ID + "}";

    public static final String FULL_ENDPOINT_CART_PRODUCTS = ENDPOINT_CARTS + SUB_ENDPOINT_CART_PRODUCTS;
    public static final String SUB_ENDPOINT_CHECKOUT_STATUS = ENDPOINT_CARTS + "/{" + ApiFieldConstants.PARAM_CART_ID + "}/checkout/status";
    // ==========================================
    // SECURITY CONFIG (SecurityConfig)
    // ==========================================

    public static final String[] PUBLIC_PATHS = {
            ENDPOINT_AUTH + "/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/actuator/health",
            "/actuator/prometheus"
    };
}