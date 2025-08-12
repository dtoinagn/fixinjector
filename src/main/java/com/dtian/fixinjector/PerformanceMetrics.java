package com.dtian.fixinjector;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerformanceMetrics {
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong bytesProcessed = new AtomicLong(0);
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    
    private volatile Instant startTime;
    private volatile Instant lastLogTime;
    private volatile long lastMessageCount = 0;
    private volatile long lastByteCount = 0;
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;

    public void start() {
        startTime = Instant.now();
        lastLogTime = startTime;
        System.out.println("Performance metrics started at: " + startTime);
    }

    public void recordMessage(long latencyNanos) {
        messagesProcessed.incrementAndGet();
        totalLatencyNanos.add(latencyNanos);
        
        updateMinLatency(latencyNanos);
        updateMaxLatency(latencyNanos);
    }

    public void recordMessage(long latencyNanos, int messageBytes) {
        recordMessage(latencyNanos);
        bytesProcessed.addAndGet(messageBytes);
    }

    public void recordError() {
        errors.incrementAndGet();
    }

    private void updateMinLatency(long latency) {
        long currentMin = minLatencyNanos.get();
        while (latency < currentMin && !minLatencyNanos.compareAndSet(currentMin, latency)) {
            currentMin = minLatencyNanos.get();
        }
    }

    private void updateMaxLatency(long latency) {
        long currentMax = maxLatencyNanos.get();
        while (latency > currentMax && !maxLatencyNanos.compareAndSet(currentMax, latency)) {
            currentMax = maxLatencyNanos.get();
        }
    }

    public void logMetrics() {
        lock.readLock().lock();
        try {
            Instant now = Instant.now();
            long currentMessages = messagesProcessed.get();
            long currentBytes = bytesProcessed.get();
            
            Duration timeSinceStart = Duration.between(startTime, now);
            Duration timeSinceLastLog = Duration.between(lastLogTime, now);
            
            long messagesSinceLastLog = currentMessages - lastMessageCount;
            long bytesSinceLastLog = currentBytes - lastByteCount;
            
            double instantThroughput = timeSinceLastLog.toMillis() > 0 ? 
                (messagesSinceLastLog * 1000.0) / timeSinceLastLog.toMillis() : 0.0;
            
            double avgThroughput = timeSinceStart.toMillis() > 0 ? 
                (currentMessages * 1000.0) / timeSinceStart.toMillis() : 0.0;
            
            double avgLatencyMs = currentMessages > 0 ? 
                (totalLatencyNanos.sum() / (double) currentMessages) / NANOS_PER_MILLISECOND : 0.0;
            
            double minLatencyMs = minLatencyNanos.get() != Long.MAX_VALUE ? 
                minLatencyNanos.get() / (double) NANOS_PER_MILLISECOND : 0.0;
            
            double maxLatencyMs = maxLatencyNanos.get() / (double) NANOS_PER_MILLISECOND;
            
            double mbProcessed = currentBytes / (1024.0 * 1024.0);
            double mbPerSecond = timeSinceStart.toSeconds() > 0 ? 
                mbProcessed / timeSinceStart.toSeconds() : 0.0;
            
            System.out.printf(
                "Metrics - Messages: %d (%.0f/s current, %.0f/s avg) | " +
                "Latency: avg=%.2fms, min=%.2fms, max=%.2fms | " +
                "Data: %.2f MB (%.2f MB/s) | " +
                "Errors: %d | " +
                "Runtime: %d:%02d:%02d%n",
                currentMessages, instantThroughput, avgThroughput,
                avgLatencyMs, minLatencyMs, maxLatencyMs,
                mbProcessed, mbPerSecond,
                errors.get(),
                timeSinceStart.toHours(),
                timeSinceStart.toMinutesPart(),
                timeSinceStart.toSecondsPart()
            );
            
            lastLogTime = now;
            lastMessageCount = currentMessages;
            lastByteCount = currentBytes;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void stop() {
        Instant stopTime = Instant.now();
        System.out.println("Performance metrics stopped at: " + stopTime);
    }

    public void printFinalReport() {
        lock.writeLock().lock();
        try {
            Instant endTime = Instant.now();
            Duration totalDuration = Duration.between(startTime, endTime);
            
            long totalMessages = messagesProcessed.get();
            long totalBytes = bytesProcessed.get();
            long totalErrors = errors.get();
            
            double avgThroughput = totalDuration.toMillis() > 0 ? 
                (totalMessages * 1000.0) / totalDuration.toMillis() : 0.0;
            
            double avgLatencyMs = totalMessages > 0 ? 
                (totalLatencyNanos.sum() / (double) totalMessages) / NANOS_PER_MILLISECOND : 0.0;
            
            double minLatencyMs = minLatencyNanos.get() != Long.MAX_VALUE ? 
                minLatencyNanos.get() / (double) NANOS_PER_MILLISECOND : 0.0;
            
            double maxLatencyMs = maxLatencyNanos.get() / (double) NANOS_PER_MILLISECOND;
            
            double totalMB = totalBytes / (1024.0 * 1024.0);
            double avgMBPerSecond = totalDuration.toSeconds() > 0 ? 
                totalMB / totalDuration.toSeconds() : 0.0;
            
            System.out.println("\n=== FINAL PERFORMANCE REPORT ===");
            System.out.println("Total runtime: " + formatDuration(totalDuration));
            System.out.println("Messages processed: " + totalMessages);
            System.out.println("Data processed: " + String.format("%.2f MB", totalMB));
            System.out.println("Average throughput: " + String.format("%.2f messages/sec", avgThroughput));
            System.out.println("Average data rate: " + String.format("%.2f MB/sec", avgMBPerSecond));
            System.out.println("Latency statistics:");
            System.out.println("  - Average: " + String.format("%.2f ms", avgLatencyMs));
            System.out.println("  - Minimum: " + String.format("%.2f ms", minLatencyMs));
            System.out.println("  - Maximum: " + String.format("%.2f ms", maxLatencyMs));
            System.out.println("Total errors: " + totalErrors);
            System.out.println("Success rate: " + String.format("%.2f%%", 
                totalMessages > 0 ? ((totalMessages - totalErrors) * 100.0) / totalMessages : 100.0));
            System.out.println("================================");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, millis);
        } else {
            return String.format("%d.%03d sec", seconds, millis);
        }
    }

    public long getMessagesProcessed() {
        return messagesProcessed.get();
    }

    public long getBytesProcessed() {
        return bytesProcessed.get();
    }

    public long getErrors() {
        return errors.get();
    }

    public double getCurrentThroughput() {
        if (startTime == null) return 0.0;
        
        Duration elapsed = Duration.between(startTime, Instant.now());
        return elapsed.toMillis() > 0 ? 
            (messagesProcessed.get() * 1000.0) / elapsed.toMillis() : 0.0;
    }
}