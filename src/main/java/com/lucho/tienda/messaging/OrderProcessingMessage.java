package com.lucho.tienda.messaging;

/**
 * Message published to Kafka when a cart is ready
 * to be processed asynchronously.
 *
 * @param cartId identifier of the cart to process
 */
public record OrderProcessingMessage(Long cartId) {
}