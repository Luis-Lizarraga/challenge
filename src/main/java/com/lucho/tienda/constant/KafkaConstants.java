package com.lucho.tienda.constant;

public final class KafkaConstants {

    // Compact constructor to prevent instantiation
    private KafkaConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String ORDER_PROCESSING_TOPIC = "order-processing";
}