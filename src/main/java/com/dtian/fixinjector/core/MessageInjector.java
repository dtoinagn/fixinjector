package com.dtian.fixinjector.core;

import com.dtian.fixinjector.config.InjectorConfig;
import com.dtian.fixinjector.model.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles injection of messages into TCP sockets.
 * Supports both individual message injection and batching.
 */
public class MessageInjector {
    private final InjectorConfig config;
    private SocketChannel socketChannel;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final BlockingQueue<Message> messageQueue;
    private final Object writeLock = new Object();

    public MessageInjector(InjectorConfig config) {
        this.config = config;
        this.messageQueue = config.isBatchingEnabled() ? 
            new ArrayBlockingQueue<>(config.getBatchSize() * 2) : null;
    }

    public void connect() throws IOException {
        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.socket().setSoTimeout(config.getSocketTimeout());
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setSendBufferSize(config.getBufferSize());
        
        System.out.println("Connecting to " + address + "...");
        
        try {
            socketChannel.connect(address);
            connected.set(true);
            System.out.println("Connected to " + address);
        } catch (IOException e) {
            close();
            throw new IOException("Failed to connect to " + address, e);
        }
    }

    public void inject(Message message) throws IOException {
        if (!connected.get()) {
            throw new IOException("Socket not connected");
        }

        if (config.isBatchingEnabled()) {
            try {
                messageQueue.put(message);
                if (messageQueue.size() >= config.getBatchSize()) {
                    flushBatch();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while queueing message", e);
            }
        } else {
            sendMessage(message);
        }
    }

    private void sendMessage(Message message) throws IOException {
        synchronized (writeLock) {
            ByteBuffer buffer = message.getByteBuffer();
            
            while (buffer.hasRemaining()) {
                int bytesWritten = socketChannel.write(buffer);
                if (bytesWritten == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during write", e);
                    }
                }
            }
        }
    }

    private void flushBatch() throws IOException {
        synchronized (writeLock) {
            int batchSize = Math.min(messageQueue.size(), config.getBatchSize());
            ByteBuffer batchBuffer = ByteBuffer.allocateDirect(batchSize * 1024);

            for (int i = 0; i < batchSize; i++) {
                Message message = messageQueue.poll();
                if (message == null) break;

                byte[] messageBytes = message.getMessageBytes();
                if (batchBuffer.remaining() < messageBytes.length) {
                    writeBatchBuffer(batchBuffer);
                    batchBuffer.clear();
                }
                batchBuffer.put(messageBytes);
            }

            if (batchBuffer.position() > 0) {
                writeBatchBuffer(batchBuffer);
            }
        }
    }

    private void writeBatchBuffer(ByteBuffer buffer) throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            int bytesWritten = socketChannel.write(buffer);
            if (bytesWritten == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during batch write", e);
                }
            }
        }
    }

    public void flushPending() throws IOException {
        if (config.isBatchingEnabled() && messageQueue != null && !messageQueue.isEmpty()) {
            flushBatch();
        }
    }

    public boolean isConnected() {
        return connected.get() && socketChannel != null && socketChannel.isConnected();
    }

    public void close() {
        connected.set(false);
        
        if (config.isBatchingEnabled() && messageQueue != null && !messageQueue.isEmpty()) {
            try {
                flushBatch();
            } catch (IOException e) {
                System.err.println("Failed to flush pending messages: " + e.getMessage());
            }
        }

        if (socketChannel != null) {
            try {
                socketChannel.close();
                System.out.println("Socket connection closed");
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public String getConnectionInfo() {
        if (socketChannel != null && socketChannel.isConnected()) {
            try {
                return socketChannel.getLocalAddress() + " -> " + socketChannel.getRemoteAddress();
            } catch (IOException e) {
                return "Connection info unavailable";
            }
        }
        return "Not connected";
    }
}