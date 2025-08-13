package com.dtian.fixinjector.core;

import com.dtian.fixinjector.config.InjectorConfig;
import com.dtian.fixinjector.metrics.PerformanceMetrics;
import com.dtian.fixinjector.model.Message;

/**
 * Processes individual messages with rate limiting and metrics tracking.
 * Coordinates between message reading and injection.
 */
public class MessageProcessor {
    private final InjectorConfig config;
    private final MessageInjector injector;
    private final PerformanceMetrics metrics;

    public MessageProcessor(InjectorConfig config, MessageInjector injector, PerformanceMetrics metrics) {
        this.config = config;
        this.injector = injector;
        this.metrics = metrics;
    }

    /**
     * Process a single message: inject it and record metrics
     */
    public void processMessage(Message message) {
        try {
            long startTime = System.nanoTime();
            injector.inject(message);
            long endTime = System.nanoTime();
            
            metrics.recordMessage(endTime - startTime);
            
            // Apply rate limiting
            long delay = calculateDelay();
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (Exception e) {
            System.err.println("Failed to inject message: " + e.getMessage());
            metrics.recordError();
        }
    }

    private long calculateDelay() {
        if (config.getInjectionRate() <= 0) {
            return 0;
        }
        return 1000 / config.getInjectionRate();
    }
}