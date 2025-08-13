package com.dtian.fixinjector.protocol;

/**
 * Configuration interface for message readers.
 * Contains settings needed by protocol implementations for reading messages.
 */
public interface MessageReaderConfig {
    int getBufferSize();
    boolean isValidationEnabled();
    int getHeaderLength(); // For protocols that need header length
    boolean isRecursiveEnabled();
    int getMaxRecursionDepth();
}