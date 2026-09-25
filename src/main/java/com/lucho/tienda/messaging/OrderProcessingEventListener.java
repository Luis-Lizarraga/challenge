package com.lucho.tienda.messaging;

import com.lucho.tienda.messaging.producer.OrderProcessingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingEventListener {

    private final OrderProcessingProducer orderProcessingProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderProcessingRequestedEvent event) {

        log.debug(
                "Transaction committed. Publishing Kafka message for cart ID: {}",
                event.cartId()
        );

        orderProcessingProducer.send(event.cartId());
    }
}