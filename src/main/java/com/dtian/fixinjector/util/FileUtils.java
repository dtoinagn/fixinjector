package com.dtian.fixinjector.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * Utility class for file operations used by protocol implementations
 */
public class FileUtils {
    
    /**
     * Read a text file line by line
     */
    public static void readTextFile(Path filePath, Consumer<String> lineConsumer, int bufferSize) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            StringBuilder messageBuilder = new StringBuilder();

            long totalBytes = channel.size();
            long processedBytes = 0;

            while (channel.read(buffer) > 0) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    processedBytes++;

                    if (b == '\n' || b == '\r') {
                        if (messageBuilder.length() > 0) {
                            lineConsumer.accept(messageBuilder.toString());
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
                lineConsumer.accept(messageBuilder.toString());
            }
        }
    }
    
    /**
     * Read a binary file and extract messages based on header length
     */
    public static void readBinaryFile(Path filePath, Consumer<byte[]> messageConsumer, int bufferSize, int headerLength) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            ByteBuffer messageBuffer = ByteBuffer.allocate(bufferSize);

            long totalBytes = channel.size();
            long processedBytes = 0;

            System.out.println("Reading binary file: " + filePath + " (" + totalBytes + " bytes)");

            while (channel.read(buffer) > 0) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    if (messageBuffer.remaining() < headerLength + 1024) {
                        messageBuffer.compact();
                        if (messageBuffer.capacity() < headerLength + 1024) {
                            ByteBuffer newBuffer = ByteBuffer.allocate(messageBuffer.capacity() * 2);
                            messageBuffer.flip();
                            newBuffer.put(messageBuffer);
                            messageBuffer = newBuffer;
                        }
                    }

                    int bytesToRead = Math.min(buffer.remaining(), messageBuffer.remaining());
                    for (int i = 0; i < bytesToRead; i++) {
                        messageBuffer.put(buffer.get());
                        processedBytes++;
                    }

                    extractBinaryMessages(messageBuffer, messageConsumer, headerLength);
                }

                buffer.clear();

                if (processedBytes % (1024 * 1024) == 0) {
                    System.out.printf("Progress: %.1f%% (%d/%d bytes)\n",
                            (processedBytes * 100.0 / totalBytes), processedBytes, totalBytes);
                }
            }

            extractBinaryMessages(messageBuffer, messageConsumer, headerLength);
            System.out.println("Finished reading binary file");
        }
    }
    
    private static void extractBinaryMessages(ByteBuffer buffer, Consumer<byte[]> messageConsumer, int headerLength) {
        buffer.flip();

        while (buffer.remaining() >= headerLength) {
            buffer.mark();

            byte[] header = new byte[headerLength];
            buffer.get(header);

            int xmlLength = buffer.remaining();
            
            if (xmlLength > 0) {
                byte[] fullMessage = new byte[headerLength + xmlLength];
                buffer.reset();
                buffer.get(fullMessage);

                messageConsumer.accept(fullMessage);
                break;
            } else {
                buffer.reset();
                break;
            }
        }

        buffer.compact();
    }
    
    /**
     * Read a gzipped file
     */
    public static void readGzipFile(Path filePath, Consumer<String> lineConsumer) throws IOException {
        System.out.println("Reading compressed file: " + filePath);

        try (InputStream fileStream = Files.newInputStream(filePath);
                GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8))) {

            String line;
            long lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                lineConsumer.accept(line);

                if (lineCount % 10000 == 0) {
                    System.out.printf("Processed %d lines from compressed file\n", lineCount);
                }
            }

            System.out.printf("Finished reading compressed file: %d lines processed\n", lineCount);
        }
    }
}