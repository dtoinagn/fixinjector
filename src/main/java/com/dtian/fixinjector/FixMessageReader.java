package com.dtian.fixinjector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class FixMessageReader {
    private final Configuration config;
    private final int bufferSize;

    public FixMessageReader(Configuration config) {
        this.config = config;
        this.bufferSize = config.getBufferSize();
    }

    public void readMessages(Consumer<FixMessage> messageConsumer) throws IOException {
        Path inputPath = Paths.get(config.getInputFile());

        if (Files.isDirectory(inputPath)) {
            readFromDirectory(inputPath, messageConsumer);
        } else {
            readFromFile(inputPath, messageConsumer);
        }
    }

    private void readFromDirectory(Path directory, Consumer<FixMessage> messageConsumer) throws IOException {
        List<Path> files = new ArrayList<>();

        if (config.isRecursiveEnabled()) {
            // Recursive traversal using Files.walk() with depth limit
            int maxDepth = config.getMaxRecursionDepth();
            try (Stream<Path> pathStream = Files.walk(directory, maxDepth)) {
                files = pathStream
                        .filter(Files::isRegularFile)
                        .filter(this::isValidFixFile)
                        .sorted()
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
            System.out.println("Found " + files.size() + " files recursively in directory tree: " + directory);
        } else {
            // Non-recursive traversal (current behavior)
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory,
                    config.getDirectoryStreamPattern())) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        files.add(file);
                    }
                }
            }
            files.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
            System.out.println("Found " + files.size() + " files in directory: " + directory);
        }

        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            // String relativePath = directory.relativize(file).toString();
            // System.out.printf("Processing file %d/%d: %s\n", i + 1, files.size(),
            // relativePath);
            readFromFile(file, messageConsumer);
        }

        System.out.println("Finished reading all files from directory" +
                (config.isRecursiveEnabled() ? " tree" : ""));
    }

    private boolean isValidFixFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        List<String> supportedExtensions = config.getSupportedFileExtensions();

        for (String extension : supportedExtensions) {
            if (fileName.endsWith("." + extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void readFromFile(Path filePath, Consumer<FixMessage> messageConsumer) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".gz")) {
            readFromGzipFile(filePath, messageConsumer);
        } else {
            readFromTextFile(filePath, messageConsumer);
        }
    }

    private void readFromGzipFile(Path filePath, Consumer<FixMessage> messageConsumer) throws IOException {
        System.out.println("Reading compressed file: " + filePath);

        try (InputStream fileStream = Files.newInputStream(filePath);
                GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8))) {

            String line;
            long lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                String messageStr = line.trim();

                if (!messageStr.isEmpty() && FixMessage.isValidFixMessage(messageStr)) {
                    FixMessage message = new FixMessage(messageStr);
                    if (!config.isValidationEnabled() || message.isValid()) {
                        messageConsumer.accept(message);
                    }
                }

                if (lineCount % 10000 == 0) {
                    System.out.printf("Processed %d lines from compressed file\n", lineCount);
                }
            }

            System.out.printf("Finished reading compressed file: %d lines processed\n", lineCount);
        }
    }

    private void readFromTextFile(Path filePath, Consumer<FixMessage> messageConsumer) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            StringBuilder messageBuilder = new StringBuilder();

            long totalBytes = channel.size();
            long processedBytes = 0;

            // System.out.println("Reading messages from: " + filePath + " (" + totalBytes +
            // " bytes)");

            while (channel.read(buffer) > 0) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    processedBytes++;

                    if (b == '\n' || b == '\r') {
                        if (messageBuilder.length() > 0) {
                            String messageStr = messageBuilder.toString().trim();
                            if (!messageStr.isEmpty() && FixMessage.isValidFixMessage(messageStr)) {
                                FixMessage message = new FixMessage(messageStr);
                                if (!config.isValidationEnabled() || message.isValid()) {
                                    messageConsumer.accept(message);
                                }
                            }
                            messageBuilder.setLength(0);
                        }
                    } else {
                        messageBuilder.append((char) b);
                    }
                }

                buffer.clear();

                if (processedBytes % (1024 * 1024) == 0) {
                    System.out.printf("Progress: %.1f%% (%d/%d bytes)\n",
                            (processedBytes * 100.0 / totalBytes), processedBytes, totalBytes);
                }
            }

            if (messageBuilder.length() > 0) {
                String messageStr = messageBuilder.toString().trim();
                if (!messageStr.isEmpty() && FixMessage.isValidFixMessage(messageStr)) {
                    FixMessage message = new FixMessage(messageStr);
                    if (!config.isValidationEnabled() || message.isValid()) {
                        messageConsumer.accept(message);
                    }
                }
            }

            // System.out.println("Finished reading text file");
        }
    }

    public long countMessages() throws IOException {
        Path inputPath = Paths.get(config.getInputFile());
        final long[] totalCount = { 0 };

        Consumer<FixMessage> counter = message -> totalCount[0]++;

        if (Files.isDirectory(inputPath)) {
            readFromDirectory(inputPath, counter);
        } else {
            readFromFile(inputPath, counter);
        }

        return totalCount[0];
    }

    public void readMessagesFromString(String content, Consumer<FixMessage> messageConsumer) {
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            String messageStr = line.trim();
            if (!messageStr.isEmpty() && FixMessage.isValidFixMessage(messageStr)) {
                FixMessage message = new FixMessage(messageStr);
                if (!config.isValidationEnabled() || message.isValid()) {
                    messageConsumer.accept(message);
                }
            }
        }
    }
}