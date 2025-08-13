package com.dtian.fixinjector.protocol.impl;

import com.dtian.fixinjector.model.FixMessage;
import com.dtian.fixinjector.model.Message;
import com.dtian.fixinjector.protocol.MessageProtocol;
import com.dtian.fixinjector.protocol.MessageReaderConfig;
import com.dtian.fixinjector.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * FIX protocol implementation
 */
public class FixProtocol implements MessageProtocol {

    @Override
    public String getProtocolName() {
        return "FIX";
    }

    @Override
    public boolean isValidMessage(byte[] data) {
        String message = new String(data);
        return FixMessage.isValidFixMessage(message);
    }

    @Override
    public Message parseMessage(byte[] data) throws IOException {
        String message = new String(data);
        if (!isValidMessage(data)) {
            throw new IOException("Invalid FIX message format");
        }
        return new FixMessage(message);
    }

    @Override
    public Message parseMessage(String data) throws IOException {
        if (!FixMessage.isValidFixMessage(data)) {
            throw new IOException("Invalid FIX message format");
        }
        return new FixMessage(data);
    }

    @Override
    public void readMessagesFromFile(Path filePath, Consumer<Message> messageConsumer, MessageReaderConfig config) throws IOException {
        FileUtils.readTextFile(filePath, line -> {
            String messageStr = line.trim();
            if (!messageStr.isEmpty() && FixMessage.isValidFixMessage(messageStr)) {
                try {
                    Message message = parseMessage(messageStr);
                    if (!config.isValidationEnabled() || message.isValid()) {
                        messageConsumer.accept(message);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to parse FIX message: " + e.getMessage());
                }
            }
        }, config.getBufferSize());
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"txt", "fix", "log"};
    }
}