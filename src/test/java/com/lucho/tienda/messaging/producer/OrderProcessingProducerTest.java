package com.lucho.tienda.messaging.producer;

import com.lucho.tienda.messaging.OrderProcessingMessage;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static com.lucho.tienda.constant.KafkaConstants.ORDER_PROCESSING_TOPIC;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingProducerTest {

    private static final Long CART_ID = 1L;

    @Mock
    private KafkaTemplate<String, OrderProcessingMessage> kafkaTemplate;

    @InjectMocks
    private OrderProcessingProducer orderProcessingProducer;

    @Test
    void send_PublishesOrderProcessingMessage() {

        OrderProcessingMessage expectedMessage =
                new OrderProcessingMessage(CART_ID);

        SendResult<String, OrderProcessingMessage> sendResult =
                mock(SendResult.class);

        RecordMetadata metadata =
                new RecordMetadata(
                        new TopicPartition(ORDER_PROCESSING_TOPIC, 0),
                        0,
                        0,
                        0,
                        0,
                        0
                );

        when(sendResult.getRecordMetadata())
                .thenReturn(metadata);

        when(kafkaTemplate.send(
                ORDER_PROCESSING_TOPIC,
                CART_ID.toString(),
                expectedMessage
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        orderProcessingProducer.send(CART_ID);

        verify(kafkaTemplate).send(
                ORDER_PROCESSING_TOPIC,
                CART_ID.toString(),
                expectedMessage
        );
    }

    @Test
    void send_CompletesSuccessfully_WhenKafkaAcknowledgesMessage() {

        OrderProcessingMessage expectedMessage =
                new OrderProcessingMessage(CART_ID);

        SendResult<String, OrderProcessingMessage> sendResult =
                mock(SendResult.class);

        RecordMetadata metadata =
                new RecordMetadata(
                        new TopicPartition(ORDER_PROCESSING_TOPIC, 0),
                        0,
                        10,
                        0,
                        0,
                        0
                );

        when(sendResult.getRecordMetadata())
                .thenReturn(metadata);

        when(kafkaTemplate.send(
                ORDER_PROCESSING_TOPIC,
                CART_ID.toString(),
                expectedMessage
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        orderProcessingProducer.send(CART_ID);

        verify(kafkaTemplate).send(
                ORDER_PROCESSING_TOPIC,
                CART_ID.toString(),
                expectedMessage
        );
    }

    @Test
    void send_ThrowsException_WhenKafkaPublishFails() {

        OrderProcessingMessage expectedMessage =
                new OrderProcessingMessage(CART_ID);

        CompletableFuture<SendResult<String, OrderProcessingMessage>> future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaTemplate.send(
                ORDER_PROCESSING_TOPIC,
                CART_ID.toString(),
                expectedMessage
        )).thenReturn(future);

        assertThrows(
                IllegalStateException.class,
                () -> orderProcessingProducer.send(CART_ID)
        );

        verify(kafkaTemplate).send(
                ORDER_PROCESSING_TOPIC,
                CART_ID.toString(),
                expectedMessage
        );
    }
}