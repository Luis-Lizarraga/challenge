package com.lucho.tienda.messaging;

import com.lucho.tienda.messaging.producer.OrderProcessingProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class OrderProcessingAfterCommitTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private OrderProcessingProducer orderProcessingProducer;

    @Test
    void event_ShouldPublishToKafkaOnlyAfterTransactionCommits() {

        transactionTemplate.executeWithoutResult(status -> {

            applicationEventPublisher.publishEvent(
                    new OrderProcessingRequestedEvent(1L)
            );

            // Todavía estamos dentro de la transacción.
            verify(orderProcessingProducer, never()).send(1L);
        });

        // La transacción ya hizo COMMIT.
        verify(orderProcessingProducer).send(1L);
    }
}