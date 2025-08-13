package com.dtian.fixinjector.protocol.impl;

import com.dtian.fixinjector.model.ByteHeaderXmlMessage;
import com.dtian.fixinjector.model.Message;
import com.dtian.fixinjector.protocol.MessageProtocol;
import com.dtian.fixinjector.protocol.MessageReaderConfig;
import com.dtian.fixinjector.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Byte header + XML payload protocol implementation
 */
public class ByteHeaderXmlProtocol implements MessageProtocol {

    @Override
    public String getProtocolName() {
        return "BYTE_HEADER_XML";
    }

    @Override
    public boolean isValidMessage(byte[] data) {
        // Basic validation - ensure we have enough data for header + payload
        return data != null && data.length > 0;
    }

    @Override
    public Message parseMessage(byte[] data) throws IOException {
        if (!isValidMessage(data)) {
            throw new IOException("Invalid byte header XML message format");
        }
        
        // For binary data, assume it's already in the correct format
        return new ByteHeaderXmlMessage(data, Math.min(8, data.length)); // Default header length
    }

    @Override
    public Message parseMessage(String data) throws IOException {
        // For text input, create a dummy header and use the text as XML payload
        byte[] dummyHeader = new byte[8]; // Default header size
        return new ByteHeaderXmlMessage(dummyHeader, data);
    }

    @Override
    public void readMessagesFromFile(Path filePath, Consumer<Message> messageConsumer, MessageReaderConfig config) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".xml") || fileName.endsWith(".txt")) {
            // Read as text file with XML content
            FileUtils.readTextFile(filePath, line -> {
                String messageStr = line.trim();
                if (!messageStr.isEmpty()) {
                    try {
                        Message message = parseMessage(messageStr);
                        if (!config.isValidationEnabled() || message.isValid()) {
                            messageConsumer.accept(message);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to parse XML message: " + e.getMessage());
                    }
                }
            }, config.getBufferSize());
        } else {
            // Read as binary file
            FileUtils.readBinaryFile(filePath, data -> {
                try {
                    Message message = new ByteHeaderXmlMessage(data, config.getHeaderLength());
                    if (!config.isValidationEnabled() || message.isValid()) {
                        messageConsumer.accept(message);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse binary message: " + e.getMessage());
                }
            }, config.getBufferSize(), config.getHeaderLength());
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"xml", "bin", "dat"};
    }
}