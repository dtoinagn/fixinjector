package com.dtian.fixinjector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FixMessageInjector {
    private final Configuration config;
    private final FixMessageReader reader;
    private final SocketInjector injector;
    private final PerformanceMetrics metrics;
    private final ExecutorService executor;

    public FixMessageInjector(Configuration config) {
        this.config = config;
        this.reader = new FixMessageReader(config);
        this.injector = new SocketInjector(config);
        this.metrics = new PerformanceMetrics();
        this.executor = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
                printUsage();
                return;
            }
            
            Configuration config = Configuration.load(args);
            
            if (config.isServerMode()) {
                System.out.println("Starting in server mode...");
                FixMessageServer server = new FixMessageServer(config);
                server.start();
                return;
            }
            
            FixMessageInjector app = new FixMessageInjector(config);
            
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
        System.out.println("FIX Message Injector - High-performance FIX message streaming tool");
        System.out.println();
        System.out.println("Usage: java -jar demo.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --file <path>         Input file/directory/gz file (default: c:\\share\\data\\fix-messages.txt)");
        System.out.println("  --host <hostname>     Target host (default: localhost)");
        System.out.println("  --port <port>         Target port (default: 9999)");
        System.out.println("  --rate <msgs/sec>     Injection rate (default: 1000)");
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
        System.out.println("  java -jar demo.jar --server");
        System.out.println("  java -jar demo.jar --host 192.168.1.100 --port 8080");
    }

    public void run() throws Exception {
        System.out.println("Starting FIX Message Injector...");
        System.out.println("Input file: " + config.getInputFile());
        System.out.println("Target: " + config.getHost() + ":" + config.getPort());
        System.out.println("Target rate: " + config.getInjectionRate() + " msgs/sec");

        metrics.start();
        injector.connect();

        CompletableFuture<Void> readerTask = CompletableFuture.runAsync(() -> {
            try {
                reader.readMessages(this::processMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        CompletableFuture<Void> metricsTask = CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    metrics.logMetrics();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, executor);

        readerTask.get();
        injector.close();
        metrics.stop();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("Injection completed.");
        metrics.printFinalReport();
    }
    
    public void shutdown() {
        try {
            if (injector != null) {
                injector.close();
            }
            if (metrics != null) {
                metrics.stop();
            }
            if (executor != null) {
                executor.shutdown();
                executor.awaitTermination(2, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }

    private void processMessage(FixMessage message) {
        try {
            long startTime = System.nanoTime();
            injector.inject(message);
            long endTime = System.nanoTime();
            
            metrics.recordMessage(endTime - startTime);
            
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