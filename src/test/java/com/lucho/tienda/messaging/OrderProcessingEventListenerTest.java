package com.lucho.tienda.messaging;

import com.lucho.tienda.messaging.producer.OrderProcessingProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderProcessingEventListenerTest {

    @Mock
    private OrderProcessingProducer orderProcessingProducer;

    @InjectMocks
    private OrderProcessingEventListener listener;

    @Test
    void handle_PublishesKafkaMessage() {

        OrderProcessingRequestedEvent event =
                new OrderProcessingRequestedEvent(1L);

        listener.handle(event);

        verify(orderProcessingProducer).send(1L);
    }
}