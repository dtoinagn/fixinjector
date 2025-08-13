package com.dtian.fixinjector.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks performance metrics for message injection.
 * Thread-safe implementation with real-time metrics calculation.
 */
public class PerformanceMetrics {
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(0);
    private final AtomicLong dataBytes = new AtomicLong(0);
    
    private volatile Instant startTime;
    private volatile Instant lastLogTime;
    private final AtomicLong lastMessageCount = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start() {
        startTime = Instant.now();
        lastLogTime = startTime;
        running.set(true);
        System.out.println("Performance metrics started at: " + startTime);
    }

    public void stop() {
        running.set(false);
        System.out.println("Performance metrics stopped at: " + Instant.now());
    }

    public void recordMessage(long latencyNanos) {
        messageCount.incrementAndGet();
        totalLatencyNanos.addAndGet(latencyNanos);
        
        // Update min latency
        minLatencyNanos.updateAndGet(current -> Math.min(current, latencyNanos));
        
        // Update max latency
        maxLatencyNanos.updateAndGet(current -> Math.max(current, latencyNanos));
    }

    public void recordMessage(long latencyNanos, int messageSize) {
        recordMessage(latencyNanos);
        dataBytes.addAndGet(messageSize);
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public void logMetrics() {
        if (!running.get()) return;

        Instant now = Instant.now();
        long currentMessages = messageCount.get();
        long currentErrors = errorCount.get();
        long currentDataBytes = dataBytes.get();
        
        // Calculate rates
        Duration totalRuntime = Duration.between(startTime, now);
        Duration intervalDuration = Duration.between(lastLogTime, now);
        
        double totalRuntimeSeconds = totalRuntime.toMillis() / 1000.0;
        double intervalSeconds = intervalDuration.toMillis() / 1000.0;
        
        long intervalMessages = currentMessages - lastMessageCount.get();
        double currentRate = intervalSeconds > 0 ? intervalMessages / intervalSeconds : 0;
        double averageRate = totalRuntimeSeconds > 0 ? currentMessages / totalRuntimeSeconds : 0;
        
        // Calculate latency statistics
        double avgLatencyMs = currentMessages > 0 ? 
            (totalLatencyNanos.get() / (double) currentMessages) / 1_000_000.0 : 0;
        double minLatencyMs = minLatencyNanos.get() != Long.MAX_VALUE ? 
            minLatencyNanos.get() / 1_000_000.0 : 0;
        double maxLatencyMs = maxLatencyNanos.get() / 1_000_000.0;
        
        // Calculate data rates
        double dataMB = currentDataBytes / (1024.0 * 1024.0);
        double dataRateMBs = totalRuntimeSeconds > 0 ? dataMB / totalRuntimeSeconds : 0;
        
        // Calculate success rate
        double successRate = currentMessages + currentErrors > 0 ? 
            (currentMessages * 100.0) / (currentMessages + currentErrors) : 100.0;

        System.out.printf("Metrics - Messages: %d (%.0f/s current, %.0f/s avg) | " +
                         "Latency: avg=%.2fms, min=%.2fms, max=%.2fms | " +
                         "Data: %.2f MB (%.2f MB/s) | " +
                         "Errors: %d | " +
                         "Runtime: %s%n",
                currentMessages, currentRate, averageRate,
                avgLatencyMs, minLatencyMs, maxLatencyMs,
                dataMB, dataRateMBs,
                currentErrors,
                formatDuration(totalRuntime));

        // Update tracking variables
        lastLogTime = now;
        lastMessageCount.set(currentMessages);
    }

    public void printFinalReport() {
        if (startTime == null) return;

        Instant endTime = Instant.now();
        Duration totalRuntime = Duration.between(startTime, endTime);
        double totalRuntimeSeconds = totalRuntime.toMillis() / 1000.0;

        long finalMessages = messageCount.get();
        long finalErrors = errorCount.get();
        long finalDataBytes = dataBytes.get();
        
        double avgThroughput = totalRuntimeSeconds > 0 ? finalMessages / totalRuntimeSeconds : 0;
        double avgLatencyMs = finalMessages > 0 ? 
            (totalLatencyNanos.get() / (double) finalMessages) / 1_000_000.0 : 0;
        double minLatencyMs = minLatencyNanos.get() != Long.MAX_VALUE ? 
            minLatencyNanos.get() / 1_000_000.0 : 0;
        double maxLatencyMs = maxLatencyNanos.get() / 1_000_000.0;
        double dataMB = finalDataBytes / (1024.0 * 1024.0);
        double dataRateMBs = totalRuntimeSeconds > 0 ? dataMB / totalRuntimeSeconds : 0;
        double successRate = finalMessages + finalErrors > 0 ? 
            (finalMessages * 100.0) / (finalMessages + finalErrors) : 100.0;

        System.out.println("\n=== FINAL PERFORMANCE REPORT ===");
        System.out.printf("Total runtime: %.3f sec%n", totalRuntimeSeconds);
        System.out.printf("Messages processed: %d%n", finalMessages);
        System.out.printf("Data processed: %.2f MB%n", dataMB);
        System.out.printf("Average throughput: %.2f messages/sec%n", avgThroughput);
        System.out.printf("Average data rate: %.2f MB/sec%n", dataRateMBs);
        System.out.println("Latency statistics:");
        System.out.printf("  - Average: %.2f ms%n", avgLatencyMs);
        System.out.printf("  - Minimum: %.2f ms%n", minLatencyMs);
        System.out.printf("  - Maximum: %.2f ms%n", maxLatencyMs);
        System.out.printf("Total errors: %d%n", finalErrors);
        System.out.printf("Success rate: %.2f%%%n", successRate);
        System.out.println("================================");
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    // Getters for programmatic access
    public long getMessageCount() { return messageCount.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public double getAverageLatencyMs() { 
        long messages = messageCount.get();
        return messages > 0 ? (totalLatencyNanos.get() / (double) messages) / 1_000_000.0 : 0;
    }
    public boolean isRunning() { return running.get(); }
}