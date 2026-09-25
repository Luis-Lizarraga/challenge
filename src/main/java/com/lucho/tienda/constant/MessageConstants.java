package com.lucho.tienda.constant;

public final class MessageConstants {

    private MessageConstants() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated"
        );
    }

    public static final String CHECKOUT_NOT_STARTED =
            "Checkout has not been started";

    public static final String PROCESSING_ORDER =
            "We are processing your order";

    public static final String ORDER_PROCESSED =
            "Order processed successfully";

    public static final String ORDER_PROCESSING_FAILED =
            "Order processing failed";

    public static final String ORDER_CANCELLED =
            "Order was cancelled";
}