package com.dtian.fixinjector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class FixMessageServer {
    private final Configuration config;
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private volatile boolean running = true;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private final Path outputDirectory;

    public FixMessageServer(Configuration config) throws IOException {
        this.config = config;
        this.outputDirectory = Paths.get(config.getOutputDirectory());
        
        if (!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
            System.out.println("Created output directory: " + outputDirectory);
        }
    }

    public static void main(String[] args) {
        try {
            System.setProperty("server.mode", "true");
            Configuration config = Configuration.load(args);
            FixMessageServer server = new FixMessageServer(config);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutdown signal received. Stopping server...");
                server.stop();
            }));
            
            server.start();
        } catch (Exception e) {
            System.err.println("Server failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        
        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        serverChannel.bind(address);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("FIX Message Server started on " + address);
        System.out.println("Output directory: " + outputDirectory);
        System.out.println("Waiting for connections...");
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(config.getBufferSize());
        
        while (running) {
            try {
                int readyChannels = selector.select(1000);
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key, buffer);
                    }

                    keyIterator.remove();
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error in server loop: " + e.getMessage());
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            
            System.out.println("Client connected: " + clientChannel.getRemoteAddress());
        }
    }

    private void handleRead(SelectionKey key, ByteBuffer buffer) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        buffer.clear();
        
        try {
            int bytesRead = clientChannel.read(buffer);
            
            if (bytesRead > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                
                String content = new String(data);
                processReceivedData(content, clientChannel);
                
                bytesReceived.addAndGet(bytesRead);
            } else if (bytesRead < 0) {
                System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                key.cancel();
                clientChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Error reading from client: " + e.getMessage());
            try {
                key.cancel();
                clientChannel.close();
            } catch (IOException closeException) {
                System.err.println("Error closing client channel: " + closeException.getMessage());
            }
        }
    }

    private void processReceivedData(String data, SocketChannel clientChannel) {
        try {
            String[] lines = data.split("\\r?\\n");
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    if (FixMessage.isValidFixMessage(trimmedLine)) {
                        FixMessage message = new FixMessage(trimmedLine);
                        saveMessage(message, clientChannel);
                        long count = messageCount.incrementAndGet();
                        
                        if (count % 1000 == 0) {
                            System.out.printf("Processed %d messages (%.2f MB total)%n", 
                                count, bytesReceived.get() / (1024.0 * 1024.0));
                        }
                    } else {
                        System.out.println("Invalid FIX message received: " + trimmedLine.substring(0, Math.min(100, trimmedLine.length())));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing received data: " + e.getMessage());
        }
    }

    private void saveMessage(FixMessage message, SocketChannel clientChannel) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String clientInfo = getClientInfo(clientChannel);
        String filename = String.format("fix-messages_%s_%s.txt", timestamp, clientInfo);
        Path outputFile = outputDirectory.resolve(filename);
        
        String logEntry = String.format("[%s] %s | %s%n", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
            message.toString(),
            message.getRawMessage());
        
        try {
            Files.write(outputFile, logEntry.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write message to file " + outputFile + ": " + e.getMessage());
            
            Path fallbackFile = outputDirectory.resolve("fix-messages_" + timestamp + ".txt");
            Files.write(fallbackFile, logEntry.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        }
    }

    private String getClientInfo(SocketChannel clientChannel) {
        try {
            String remoteAddress = clientChannel.getRemoteAddress().toString();
            return remoteAddress.replaceAll("[^a-zA-Z0-9.-]", "_");
        } catch (IOException e) {
            return "unknown_client";
        }
    }

    public void stop() {
        running = false;
        
        try {
            if (selector != null) {
                selector.wakeup();
                selector.close();
            }
            if (serverChannel != null) {
                serverChannel.close();
            }
            
            System.out.println("\nServer Statistics:");
            System.out.println("Messages received: " + messageCount.get());
            System.out.println("Bytes received: " + String.format("%.2f MB", bytesReceived.get() / (1024.0 * 1024.0)));
            System.out.println("Output directory: " + outputDirectory);
            System.out.println("Server stopped successfully.");
            
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
}