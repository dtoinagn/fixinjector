package com.dtian.fixinjector.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class PerformanceMetricsTest {
    
    private PerformanceMetrics metrics;
    private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUp() {
        metrics = new PerformanceMetrics();
        System.setOut(new PrintStream(outputCapture));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
    
    @Test
    @DisplayName("Should initialize with zero counts")
    public void shouldInitializeWithZeroCounts() {
        assertEquals(0, metrics.getMessageCount());
        assertEquals(0, metrics.getErrorCount());
        assertEquals(0.0, metrics.getAverageLatencyMs(), 0.001);
        assertFalse(metrics.isRunning());
    }
    
    @Test
    @DisplayName("Should start and stop correctly")
    public void shouldStartAndStopCorrectly() {
        assertFalse(metrics.isRunning());
        
        metrics.start();
        assertTrue(metrics.isRunning());
        
        metrics.stop();
        assertFalse(metrics.isRunning());
    }
    
    @Test
    @DisplayName("Should record messages correctly")
    public void shouldRecordMessagesCorrectly() {
        metrics.recordMessage(1000000L); // 1ms in nanos
        metrics.recordMessage(2000000L); // 2ms in nanos
        metrics.recordMessage(3000000L); // 3ms in nanos
        
        assertEquals(3, metrics.getMessageCount());
        assertEquals(2.0, metrics.getAverageLatencyMs(), 0.001); // (1+2+3)/3 = 2ms
    }
    
    @Test
    @DisplayName("Should record messages with size correctly")
    public void shouldRecordMessagesWithSizeCorrectly() {
        metrics.recordMessage(1000000L, 100); // 1ms, 100 bytes
        metrics.recordMessage(2000000L, 200); // 2ms, 200 bytes
        
        assertEquals(2, metrics.getMessageCount());
        assertEquals(1.5, metrics.getAverageLatencyMs(), 0.001); // (1+2)/2 = 1.5ms
    }
    
    @Test
    @DisplayName("Should record errors correctly")
    public void shouldRecordErrorsCorrectly() {
        metrics.recordError();
        metrics.recordError();
        metrics.recordError();
        
        assertEquals(3, metrics.getErrorCount());
        assertEquals(0, metrics.getMessageCount());
    }
    
    @Test
    @DisplayName("Should handle zero messages for average latency")
    public void shouldHandleZeroMessagesForAverageLatency() {
        assertEquals(0.0, metrics.getAverageLatencyMs(), 0.001);
    }
    
    @Test
    @DisplayName("Should calculate average latency correctly")
    public void shouldCalculateAverageLatencyCorrectly() {
        metrics.recordMessage(1000000L); // 1ms
        assertEquals(1.0, metrics.getAverageLatencyMs(), 0.001);
        
        metrics.recordMessage(3000000L); // 3ms
        assertEquals(2.0, metrics.getAverageLatencyMs(), 0.001); // (1+3)/2 = 2ms
        
        metrics.recordMessage(5000000L); // 5ms
        assertEquals(3.0, metrics.getAverageLatencyMs(), 0.001); // (1+3+5)/3 = 3ms
    }
    
    @Test
    @DisplayName("Should log metrics when running")
    public void shouldLogMetricsWhenRunning() throws InterruptedException {
        metrics.start();
        metrics.recordMessage(1000000L, 100);
        metrics.recordMessage(2000000L, 200);
        
        Thread.sleep(50); // Small delay to ensure time passes
        metrics.logMetrics();
        
        String output = outputCapture.toString();
        assertTrue(output.contains("Metrics - Messages: 2"));
        assertTrue(output.contains("Latency: avg="));
        assertTrue(output.contains("Data:"));
        assertTrue(output.contains("Errors: 0"));
        assertTrue(output.contains("Runtime:"));
    }
    
    @Test
    @DisplayName("Should not log metrics when not running")
    public void shouldNotLogMetricsWhenNotRunning() {
        metrics.recordMessage(1000000L);
        metrics.logMetrics(); // Not started yet
        
        String output = outputCapture.toString();
        assertFalse(output.contains("Metrics - Messages"));
    }
    
    @Test
    @DisplayName("Should print final report correctly")
    public void shouldPrintFinalReportCorrectly() throws InterruptedException {
        metrics.start();
        metrics.recordMessage(1000000L, 100); // 1ms, 100 bytes
        metrics.recordMessage(3000000L, 200); // 3ms, 200 bytes
        metrics.recordError();
        
        Thread.sleep(50); // Ensure some runtime
        metrics.printFinalReport();
        
        String output = outputCapture.toString();
        assertTrue(output.contains("=== FINAL PERFORMANCE REPORT ==="));
        assertTrue(output.contains("Messages processed: 2"));
        assertTrue(output.contains("Average throughput:"));
        assertTrue(output.contains("Latency statistics:"));
        assertTrue(output.contains("- Average: 2.00 ms"));
        assertTrue(output.contains("Total errors: 1"));
        assertTrue(output.contains("Success rate:"));
    }
    
    @Test
    @DisplayName("Should not print final report if never started")
    public void shouldNotPrintFinalReportIfNeverStarted() {
        metrics.recordMessage(1000000L);
        metrics.printFinalReport();
        
        String output = outputCapture.toString();
        assertFalse(output.contains("=== FINAL PERFORMANCE REPORT ==="));
    }
    
    @Test
    @DisplayName("Should handle high frequency message recording")
    public void shouldHandleHighFrequencyMessageRecording() {
        int messageCount = 10000;
        long baseLatency = 1000000L; // 1ms base
        
        for (int i = 0; i < messageCount; i++) {
            metrics.recordMessage(baseLatency + (i % 1000) * 1000L, 50 + (i % 100));
        }
        
        assertEquals(messageCount, metrics.getMessageCount());
        assertTrue(metrics.getAverageLatencyMs() > 1.0); // Should be > 1ms due to variance
        assertEquals(0, metrics.getErrorCount()); // No errors recorded
    }
    
    @Test
    @DisplayName("Should calculate success rate correctly")
    public void shouldCalculateSuccessRateCorrectly() throws InterruptedException {
        metrics.start();
        
        // Record some successful messages
        for (int i = 0; i < 8; i++) {
            metrics.recordMessage(1000000L);
        }
        
        // Record some errors
        for (int i = 0; i < 2; i++) {
            metrics.recordError();
        }
        
        Thread.sleep(50);
        metrics.printFinalReport();
        
        String output = outputCapture.toString();
        assertTrue(output.contains("Success rate: 80.00%")); // 8/(8+2) = 80%
    }
    
    @Test
    @DisplayName("Should handle concurrent access safely")
    public void shouldHandleConcurrentAccessSafely() throws InterruptedException {
        metrics.start();
        
        int threadCount = 10;
        int messagesPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    metrics.recordMessage(1000000L * (threadId + 1), 100);
                    if (j % 10 == 0) {
                        metrics.recordError();
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(threadCount * messagesPerThread, metrics.getMessageCount());
        assertEquals(threadCount * (messagesPerThread / 10), metrics.getErrorCount());
        assertTrue(metrics.getAverageLatencyMs() > 0);
    }
    
    @Test
    @DisplayName("Should format duration correctly")
    public void shouldFormatDurationCorrectly() throws InterruptedException {
        metrics.start();
        metrics.recordMessage(1000000L);
        
        Thread.sleep(100); // Sleep for 100ms
        metrics.logMetrics();
        
        String output = outputCapture.toString();
        assertTrue(output.contains("Runtime: 0:")); // Should show minutes:seconds format
    }
    
    @Test
    @DisplayName("Should handle minimum and maximum latency tracking")
    public void shouldHandleMinimumAndMaximumLatencyTracking() throws InterruptedException {
        metrics.start();
        
        metrics.recordMessage(500000L);  // 0.5ms
        metrics.recordMessage(5000000L); // 5ms
        metrics.recordMessage(1000000L); // 1ms
        
        Thread.sleep(50);
        metrics.printFinalReport();
        
        String output = outputCapture.toString();
        assertTrue(output.contains("- Minimum: 0.50 ms"));
        assertTrue(output.contains("- Maximum: 5.00 ms"));
        assertTrue(output.contains("- Average: 2.17 ms")); // (0.5+5+1)/3 ≈ 2.17
    }
    
    @Test
    @DisplayName("Should track data throughput correctly")
    public void shouldTrackDataThroughputCorrectly() throws InterruptedException {
        metrics.start();
        
        // Send 1MB of data (1024 * 1024 bytes)
        int messageSize = 1024;
        for (int i = 0; i < 1024; i++) {
            metrics.recordMessage(1000000L, messageSize);
        }
        
        Thread.sleep(100); // Ensure some time passes for rate calculation
        metrics.printFinalReport();
        
        String output = outputCapture.toString();
        assertTrue(output.contains("Data processed: 1.00 MB"));
        assertTrue(output.contains("Average data rate:")); // Should show MB/s rate
    }
}