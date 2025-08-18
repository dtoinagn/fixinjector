package com.dtian.fixinjector.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class FileUtilsTest {
    
    @TempDir
    Path tempDir;
    
    private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputCapture));
    }
    
    @Test
    @DisplayName("Should read text file line by line")
    public void shouldReadTextFileLineByLine() throws IOException {
        String content = "Line 1\nLine 2\nLine 3\n";
        Path textFile = tempDir.resolve("test.txt");
        Files.write(textFile, content.getBytes());
        
        List<String> lines = new ArrayList<>();
        FileUtils.readTextFile(textFile, lines::add, 8192);
        
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }
    
    @Test
    @DisplayName("Should handle empty text file")
    public void shouldHandleEmptyTextFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.write(emptyFile, "".getBytes());
        
        List<String> lines = new ArrayList<>();
        FileUtils.readTextFile(emptyFile, lines::add, 8192);
        
        assertEquals(0, lines.size());
    }
    
    @Test
    @DisplayName("Should handle text file with different line endings")
    public void shouldHandleTextFileWithDifferentLineEndings() throws IOException {
        String content = "Line 1\rLine 2\r\nLine 3\nLine 4";
        Path textFile = tempDir.resolve("mixed-endings.txt");
        Files.write(textFile, content.getBytes());
        
        List<String> lines = new ArrayList<>();
        FileUtils.readTextFile(textFile, lines::add, 8192);
        
        assertEquals(4, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
        assertEquals("Line 4", lines.get(3));
    }
    
    @Test
    @DisplayName("Should handle text file with small buffer size")
    public void shouldHandleTextFileWithSmallBufferSize() throws IOException {
        String content = "This is a longer line that should work with small buffers\nAnother line\n";
        Path textFile = tempDir.resolve("long-lines.txt");
        Files.write(textFile, content.getBytes());
        
        List<String> lines = new ArrayList<>();
        FileUtils.readTextFile(textFile, lines::add, 10); // Very small buffer
        
        assertEquals(2, lines.size());
        assertEquals("This is a longer line that should work with small buffers", lines.get(0));
        assertEquals("Another line", lines.get(1));
    }
    
    @Test
    @DisplayName("Should handle text file without final newline")
    public void shouldHandleTextFileWithoutFinalNewline() throws IOException {
        String content = "Line 1\nLine 2\nLine 3 without newline";
        Path textFile = tempDir.resolve("no-final-newline.txt");
        Files.write(textFile, content.getBytes());
        
        List<String> lines = new ArrayList<>();
        FileUtils.readTextFile(textFile, lines::add, 8192);
        
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3 without newline", lines.get(2));
    }
    
    @Test
    @DisplayName("Should read binary file and extract messages")
    public void shouldReadBinaryFileAndExtractMessages() throws IOException {
        // Create test binary data with header + payload
        byte[] header = {0x01, 0x02, 0x03, 0x04};
        byte[] payload = "XML payload data".getBytes();
        byte[] fullMessage = new byte[header.length + payload.length];
        System.arraycopy(header, 0, fullMessage, 0, header.length);
        System.arraycopy(payload, 0, fullMessage, header.length, payload.length);
        
        Path binaryFile = tempDir.resolve("test.bin");
        Files.write(binaryFile, fullMessage);
        
        List<byte[]> messages = new ArrayList<>();
        FileUtils.readBinaryFile(binaryFile, messages::add, 8192, header.length);
        
        assertEquals(1, messages.size());
        assertArrayEquals(fullMessage, messages.get(0));
    }
    
    @Test
    @DisplayName("Should handle empty binary file")
    public void shouldHandleEmptyBinaryFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.bin");
        Files.write(emptyFile, new byte[0]);
        
        List<byte[]> messages = new ArrayList<>();
        FileUtils.readBinaryFile(emptyFile, messages::add, 8192, 4);
        
        assertEquals(0, messages.size());
    }
    
    @Test
    @DisplayName("Should handle binary file smaller than header length")
    public void shouldHandleBinaryFileSmallerThanHeaderLength() throws IOException {
        byte[] tinyData = {0x01, 0x02}; // Smaller than header length
        Path tinyFile = tempDir.resolve("tiny.bin");
        Files.write(tinyFile, tinyData);
        
        List<byte[]> messages = new ArrayList<>();
        FileUtils.readBinaryFile(tinyFile, messages::add, 8192, 4);
        
        assertEquals(0, messages.size());
    }
    
    @Test
    @DisplayName("Should handle binary file with exact header length")
    public void shouldHandleBinaryFileWithExactHeaderLength() throws IOException {
        byte[] headerOnlyData = {0x01, 0x02, 0x03, 0x04};
        Path headerOnlyFile = tempDir.resolve("header-only.bin");
        Files.write(headerOnlyFile, headerOnlyData);
        
        List<byte[]> messages = new ArrayList<>();
        FileUtils.readBinaryFile(headerOnlyFile, messages::add, 8192, 4);
        
        // FileUtils binary reader requires payload data after header, so header-only file produces 0 messages
        assertEquals(0, messages.size());
    }
    
    @Test
    @DisplayName("Should read gzip file")
    public void shouldReadGzipFile() throws IOException {
        String content = "Compressed line 1\nCompressed line 2\nCompressed line 3\n";
        
        // Create gzip file
        Path gzipFile = tempDir.resolve("test.gz");
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(gzipFile))) {
            gzipOut.write(content.getBytes());
        }
        
        List<String> lines = new ArrayList<>();
        FileUtils.readGzipFile(gzipFile, lines::add);
        
        assertEquals(3, lines.size());
        assertEquals("Compressed line 1", lines.get(0));
        assertEquals("Compressed line 2", lines.get(1));
        assertEquals("Compressed line 3", lines.get(2));
    }
    
    @Test
    @DisplayName("Should handle empty gzip file")
    public void shouldHandleEmptyGzipFile() throws IOException {
        // Create empty gzip file
        Path gzipFile = tempDir.resolve("empty.gz");
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(gzipFile))) {
            // Write nothing
        }
        
        List<String> lines = new ArrayList<>();
        FileUtils.readGzipFile(gzipFile, lines::add);
        
        assertEquals(0, lines.size());
    }
    
    @Test
    @DisplayName("Should handle large gzip file with progress reporting")
    public void shouldHandleLargeGzipFileWithProgressReporting() throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < 15000; i++) { // More than 10000 lines to trigger progress
            contentBuilder.append("Line ").append(i).append("\n");
        }
        
        Path gzipFile = tempDir.resolve("large.gz");
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(gzipFile))) {
            gzipOut.write(contentBuilder.toString().getBytes());
        }
        
        List<String> lines = new ArrayList<>();
        FileUtils.readGzipFile(gzipFile, lines::add);
        
        assertEquals(15000, lines.size());
        assertEquals("Line 0", lines.get(0));
        assertEquals("Line 14999", lines.get(14999));
        
        // Verify progress was reported
        String output = outputCapture.toString();
        assertTrue(output.contains("Processed 10000 lines"));
    }
    
    @Test
    @DisplayName("Should handle non-existent file gracefully")
    public void shouldHandleNonExistentFileGracefully() {
        Path nonExistentFile = tempDir.resolve("non-existent.txt");
        
        List<String> lines = new ArrayList<>();
        
        assertThrows(IOException.class, () -> {
            FileUtils.readTextFile(nonExistentFile, lines::add, 8192);
        });
        
        assertThrows(IOException.class, () -> {
            FileUtils.readBinaryFile(nonExistentFile, data -> {}, 8192, 4);
        });
        
        assertThrows(IOException.class, () -> {
            FileUtils.readGzipFile(nonExistentFile, line -> {});
        });
    }
    
    @Test
    @DisplayName("Should show progress for large text files")
    public void shouldShowProgressForLargeTextFiles() throws IOException {
        // Create a file larger than 1MB to trigger progress reporting
        StringBuilder contentBuilder = new StringBuilder();
        String line = "This is a test line that will be repeated many times to create a large file\n";
        int linesNeeded = (1024 * 1024 / line.length()) + 1000; // Just over 1MB
        
        for (int i = 0; i < linesNeeded; i++) {
            contentBuilder.append(line);
        }
        
        Path largeFile = tempDir.resolve("large.txt");
        Files.write(largeFile, contentBuilder.toString().getBytes());
        
        List<String> lines = new ArrayList<>();
        FileUtils.readTextFile(largeFile, lines::add, 8192);
        
        assertEquals(linesNeeded, lines.size());
        
        // Check that progress was reported
        String output = outputCapture.toString();
        assertTrue(output.contains("Progress:"));
    }
    
    @Test
    @DisplayName("Should show progress for large binary files")
    public void shouldShowProgressForLargeBinaryFiles() throws IOException {
        // Create binary file larger than 1MB
        byte[] data = new byte[1024 * 1024 + 1000]; // Just over 1MB
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        
        Path largeBinaryFile = tempDir.resolve("large.bin");
        Files.write(largeBinaryFile, data);
        
        List<byte[]> messages = new ArrayList<>();
        FileUtils.readBinaryFile(largeBinaryFile, messages::add, 8192, 4);
        
        // Check that progress was reported
        String output = outputCapture.toString();
        assertTrue(output.contains("Progress:"));
        assertTrue(output.contains("Reading binary file:"));
        assertTrue(output.contains("Finished reading binary file"));
    }
}