package com.lucho.tienda.messaging.producer;

import com.lucho.tienda.messaging.OrderProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.lucho.tienda.constant.KafkaConstants.ORDER_PROCESSING_TOPIC;

/**
 * Publishes order processing messages to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingProducer {

    private final KafkaTemplate<String, OrderProcessingMessage> kafkaTemplate;

    /**
     * Publishes a cart for asynchronous order processing.
     *
     * The cart ID is also used as the Kafka message key so messages
     * for the same cart are routed consistently to the same partition.
     *
     * @param cartId identifier of the cart to process
     */
    public void send(Long cartId) {

        OrderProcessingMessage message =
                new OrderProcessingMessage(cartId);

        try {
            SendResult<String, OrderProcessingMessage> result =
                    kafkaTemplate.send(
                            ORDER_PROCESSING_TOPIC,
                            cartId.toString(),
                            message
                    ).get(5, TimeUnit.SECONDS);

            log.info(
                    "Order processing message published for cart ID: {}. " +
                            "Topic: {}, partition: {}, offset: {}",
                    cartId,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while publishing order processing message for cart ID: "
                            + cartId,
                    ex
            );

        } catch (TimeoutException ex) {

            throw new IllegalStateException(
                    "Timeout while publishing order processing message for cart ID: "
                            + cartId,
                    ex
            );

        } catch (ExecutionException ex) {

            throw new IllegalStateException(
                    "Failed to publish order processing message for cart ID: "
                            + cartId,
                    ex.getCause()
            );
        }
    }
}