package com.dtian.fixinjector;

import com.dtian.fixinjector.config.ConfigurationManager;
import com.dtian.fixinjector.config.InjectorConfig;
import com.dtian.fixinjector.core.MessageInjector;
import com.dtian.fixinjector.core.MessageProcessor;
import com.dtian.fixinjector.core.MessageReader;
import com.dtian.fixinjector.factory.MessageProtocolFactory;
import com.dtian.fixinjector.metrics.PerformanceMetrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main application class for the Message Injector.
 * Coordinates all components and manages application lifecycle.
 */
public class Application {
    private final InjectorConfig config;
    private final MessageReader messageReader;
    private final MessageInjector messageInjector;
    private final MessageProcessor messageProcessor;
    private final PerformanceMetrics metrics;
    private final ExecutorService executor;

    public Application(InjectorConfig config) {
        this.config = config;
        this.messageReader = new MessageReader(config);
        this.messageInjector = new MessageInjector(config);
        this.metrics = new PerformanceMetrics();
        this.messageProcessor = new MessageProcessor(config, messageInjector, metrics);
        this.executor = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
                printUsage();
                return;
            }
            
            InjectorConfig config = ConfigurationManager.load(args);
            
            if (config.isServerMode()) {
                System.out.println("Starting in server mode...");
                FixMessageServer server = new FixMessageServer(config);
                server.start();
                return;
            }
            
            Application app = new Application(config);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutdown signal received. Cleaning up...");
                app.shutdown();
            }));
            
            app.run();
        } catch (Exception e) {
            System.err.println("Application failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Message Injector - High-performance message streaming tool");
        System.out.println();
        System.out.println("Usage: java -jar demo.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --file <path>         Input file/directory/gz file (default: c:\\share\\data\\fix-messages.txt)");
        System.out.println("  --host <hostname>     Target host (default: localhost)");
        System.out.println("  --port <port>         Target port (default: 9999)");
        System.out.println("  --rate <msgs/sec>     Injection rate (default: 1000)");
        System.out.println("  --protocol <name>     Message protocol (default: FIX)");
        System.out.printf ("                        Available: %s%n", String.join(", ", MessageProtocolFactory.getAvailableProtocols()));
        System.out.println("  --header-length <n>   For BYTE_HEADER_XML: header length in bytes (default: 8)");
        System.out.println("  --config <path>       Custom config file path");
        System.out.println("  --recursive           Enable recursive directory traversal (default: true)");
        System.out.println("  --no-recursive        Disable recursive directory traversal");
        System.out.println("  --server              Run as socket server (for testing)");
        System.out.println("  --help, -h            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar demo.jar --file c:\\share\\data\\messages.txt --rate 5000");
        System.out.println("  java -jar demo.jar --file c:\\share\\data\\messages.gz --rate 2000");
        System.out.println("  java -jar demo.jar --file c:\\share\\data\\ --rate 1000");
        System.out.println("  java -jar demo.jar --file c:\\share\\data\\ --no-recursive");
        System.out.println("  java -jar demo.jar --protocol BYTE_HEADER_XML --header-length 16");
        System.out.println("  java -jar demo.jar --server");
        System.out.println("  java -jar demo.jar --host 192.168.1.100 --port 8080");
    }

    public void run() throws Exception {
        printStartupInfo();

        if (config.isMetricsEnabled()) {
            metrics.start();
        }
        
        messageInjector.connect();

        // Start message reading and processing
        CompletableFuture<Void> readerTask = CompletableFuture.runAsync(() -> {
            try {
                messageReader.readMessages(messageProcessor::processMessage);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read messages", e);
            }
        }, executor);

        // Start metrics logging if enabled
        CompletableFuture<Void> metricsTask = null;
        if (config.isMetricsEnabled()) {
            metricsTask = CompletableFuture.runAsync(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(config.getMetricsInterval());
                        metrics.logMetrics();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, executor);
        }

        // Wait for reading to complete
        readerTask.join();
        
        // Flush any pending messages
        messageInjector.flushPending();

        // Stop metrics task
        if (metricsTask != null) {
            metricsTask.cancel(true);
        }

        // Shutdown gracefully
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("Injection completed.");
        if (config.isMetricsEnabled()) {
            metrics.printFinalReport();
        }
    }

    private void printStartupInfo() {
        System.out.println("Starting Message Injector...");
        System.out.println("Protocol: " + config.getMessageProtocol());
        System.out.println("Input file: " + config.getInputFile());
        System.out.println("Target: " + config.getHost() + ":" + config.getPort());
        System.out.println("Target rate: " + config.getInjectionRate() + " msgs/sec");
        
        if ("BYTE_HEADER_XML".equals(config.getMessageProtocol())) {
            System.out.println("Header length: " + config.getHeaderLength() + " bytes");
        }
        
        if (config.isBatchingEnabled()) {
            System.out.println("Batching enabled: " + config.getBatchSize() + " messages per batch");
        }
        
        System.out.println();
    }
    
    public void shutdown() {
        try {
            if (messageInjector != null) {
                messageInjector.close();
            }
            if (metrics != null && config.isMetricsEnabled()) {
                metrics.stop();
            }
            if (executor != null) {
                executor.shutdown();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            }
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}