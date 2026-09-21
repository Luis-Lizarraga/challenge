package com.lucho.tienda.constant;

public final class CartConstants {

    // Compact constructor to prevent instantiation
    private CartConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String PERCENTAGE_DIVISOR = "100";
    public static final int DEFAULT_QUANTITY = 1;
    public static final int MONEY_SCALE = 2;
}