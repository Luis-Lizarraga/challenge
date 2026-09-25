package com.lucho.tienda.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class OrderMetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter checkoutStarted;
    private final Counter checkoutProcessed;
    private final Counter checkoutFailed;
    private final Counter checkoutCancelled;

    private final Timer checkoutProcessingTimer;

    public OrderMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.checkoutStarted = Counter.builder("checkout.started")
                .description("Number of checkouts started")
                .register(meterRegistry);

        this.checkoutProcessed = Counter.builder("checkout.processed")
                .description("Number of checkouts processed successfully")
                .register(meterRegistry);

        this.checkoutFailed = Counter.builder("checkout.failed")
                .description("Number of failed checkouts")
                .register(meterRegistry);

        this.checkoutCancelled = Counter.builder("checkout.cancelled")
                .description("Number of cancelled checkouts")
                .register(meterRegistry);

        this.checkoutProcessingTimer = Timer.builder("checkout.processing.duration")
                .description("Checkout processing duration")
                .register(meterRegistry);
    }

    public void incrementStarted() {
        checkoutStarted.increment();
    }

    public void incrementProcessed() {
        checkoutProcessed.increment();
    }

    public void incrementFailed() {
        checkoutFailed.increment();
    }

    public void incrementCancelled() {
        checkoutCancelled.increment();
    }

    /**
     * Starts measuring the processing time of a checkout.
     * The returned sample must be stopped with {@link #stopTimer(Timer.Sample)}.
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stops the checkout timer and records the elapsed time.
     */
    public void stopTimer(Timer.Sample sample) {
        if (sample != null) {
            sample.stop(checkoutProcessingTimer);
        }
    }
}