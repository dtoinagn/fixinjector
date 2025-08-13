package com.dtian.fixinjector.core;

import com.dtian.fixinjector.config.InjectorConfig;
import com.dtian.fixinjector.factory.MessageProtocolFactory;
import com.dtian.fixinjector.model.Message;
import com.dtian.fixinjector.protocol.MessageProtocol;
import com.dtian.fixinjector.protocol.MessageReaderConfig;
import com.dtian.fixinjector.util.FileUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Universal message reader that supports multiple message protocols through delegation.
 * Uses the strategy pattern with protocol implementations.
 */
public class MessageReader {
    private final InjectorConfig config;
    private final MessageProtocol protocol;
    private final MessageReaderConfig readerConfig;

    public MessageReader(InjectorConfig config) {
        this.config = config;
        this.protocol = MessageProtocolFactory.createProtocol(config.getMessageProtocol());
        this.readerConfig = new MessageReaderConfigImpl(config);
    }

    public void readMessages(Consumer<Message> messageConsumer) throws IOException {
        Path inputPath = Paths.get(config.getInputFile());

        if (Files.isDirectory(inputPath)) {
            readFromDirectory(inputPath, messageConsumer);
        } else {
            readFromFile(inputPath, messageConsumer);
        }
    }

    private void readFromDirectory(Path directory, Consumer<Message> messageConsumer) throws IOException {
        List<Path> files = collectFiles(directory);
        
        System.out.println("Found " + files.size() + " files in directory" +
                (config.isRecursiveEnabled() ? " tree" : "") + ": " + directory);

        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            readFromFile(file, messageConsumer);
        }

        System.out.println("Finished reading all files from directory" +
                (config.isRecursiveEnabled() ? " tree" : ""));
    }

    private List<Path> collectFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        
        if (config.isRecursiveEnabled()) {
            int maxDepth = config.getMaxRecursionDepth();
            try (Stream<Path> pathStream = Files.walk(directory, maxDepth)) {
                files = pathStream
                        .filter(Files::isRegularFile)
                        .filter(this::isValidFile)
                        .sorted()
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, createFilePattern())) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file) && isValidFile(file)) {
                        files.add(file);
                    }
                }
            }
            files.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        }
        
        return files;
    }

    private boolean isValidFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        
        // Check protocol-specific extensions
        String[] protocolExtensions = protocol.getSupportedExtensions();
        for (String extension : protocolExtensions) {
            if (fileName.endsWith("." + extension.toLowerCase())) {
                return true;
            }
        }
        
        // Check general extensions from config
        List<String> configExtensions = config.getSupportedFileExtensions();
        for (String extension : configExtensions) {
            if (fileName.endsWith("." + extension.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    private String createFilePattern() {
        List<String> allExtensions = new ArrayList<>();
        allExtensions.addAll(Arrays.asList(protocol.getSupportedExtensions()));
        allExtensions.addAll(config.getSupportedFileExtensions());
        
        if (allExtensions.size() == 1) {
            return "*." + allExtensions.get(0);
        } else {
            return "*.{" + String.join(",", allExtensions) + "}";
        }
    }

    private void readFromFile(Path filePath, Consumer<Message> messageConsumer) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".gz")) {
            readGzipFile(filePath, messageConsumer);
        } else {
            protocol.readMessagesFromFile(filePath, messageConsumer, readerConfig);
        }
    }

    private void readGzipFile(Path filePath, Consumer<Message> messageConsumer) throws IOException {
        FileUtils.readGzipFile(filePath, line -> {
            String messageStr = line.trim();
            if (!messageStr.isEmpty()) {
                try {
                    Message message = protocol.parseMessage(messageStr);
                    if (!config.isValidationEnabled() || message.isValid()) {
                        messageConsumer.accept(message);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to parse message from gzip: " + e.getMessage());
                }
            }
        });
    }

    public long countMessages() throws IOException {
        final long[] totalCount = { 0 };
        Consumer<Message> counter = message -> totalCount[0]++;
        readMessages(counter);
        return totalCount[0];
    }

    /**
     * Implementation of MessageReaderConfig that adapts InjectorConfig
     */
    private static class MessageReaderConfigImpl implements MessageReaderConfig {
        private final InjectorConfig config;

        public MessageReaderConfigImpl(InjectorConfig config) {
            this.config = config;
        }

        @Override
        public int getBufferSize() {
            return config.getBufferSize();
        }

        @Override
        public boolean isValidationEnabled() {
            return config.isValidationEnabled();
        }

        @Override
        public int getHeaderLength() {
            return config.getHeaderLength();
        }

        @Override
        public boolean isRecursiveEnabled() {
            return config.isRecursiveEnabled();
        }

        @Override
        public int getMaxRecursionDepth() {
            return config.getMaxRecursionDepth();
        }
    }
}