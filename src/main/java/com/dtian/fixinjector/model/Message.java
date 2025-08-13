package com.dtian.fixinjector.model;

import java.nio.ByteBuffer;

/**
 * Abstract base class for all message types supported by the injector.
 * Provides common interface for different message protocols.
 */
public abstract class Message {
    protected final byte[] messageBytes;
    protected final int length;

    protected Message(byte[] messageBytes) {
        this.messageBytes = messageBytes.clone();
        this.length = messageBytes.length;
    }

    /**
     * Get the raw message bytes
     */
    public byte[] getMessageBytes() {
        return messageBytes.clone();
    }

    /**
     * Get message as ByteBuffer for efficient I/O
     */
    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(messageBytes);
    }

    /**
     * Get message length in bytes
     */
    public int getLength() {
        return length;
    }

    /**
     * Validate message format (optional, may return true for protocols without validation)
     */
    public abstract boolean isValid();

    /**
     * Get message type identifier
     */
    public abstract String getMessageType();

    /**
     * Get protocol name
     */
    public abstract String getProtocol();
}