package com.dtian.fixinjector.protocol;

import com.dtian.fixinjector.model.Message;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Interface defining the contract for message protocol implementations.
 * Each protocol (FIX, BYTE_HEADER_XML, etc.) should implement this interface.
 */
public interface MessageProtocol {
    
    /**
     * Get the protocol name/identifier
     */
    String getProtocolName();
    
    /**
     * Check if the given data represents a valid message for this protocol
     */
    boolean isValidMessage(byte[] data);
    
    /**
     * Parse a message from raw bytes
     */
    Message parseMessage(byte[] data) throws IOException;
    
    /**
     * Parse a message from string (for text-based protocols)
     */
    default Message parseMessage(String data) throws IOException {
        return parseMessage(data.getBytes());
    }
    
    /**
     * Read messages from a file and process them with the given consumer
     */
    void readMessagesFromFile(Path filePath, Consumer<Message> messageConsumer, MessageReaderConfig config) throws IOException;
    
    /**
     * Get supported file extensions for this protocol
     */
    String[] getSupportedExtensions();
}