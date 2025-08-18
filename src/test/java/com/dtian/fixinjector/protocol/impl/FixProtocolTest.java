package com.dtian.fixinjector.protocol.impl;

import com.dtian.fixinjector.model.FixMessage;
import com.dtian.fixinjector.model.Message;
import com.dtian.fixinjector.protocol.MessageReaderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class FixProtocolTest {
    
    private FixProtocol protocol;
    
    @TempDir
    Path tempDir;
    
    private static final String VALID_FIX_MESSAGE = "8=FIX.4.2\0019=154\00135=D\00149=SENDER\00156=TARGET\00134=1\00152=20231201-10:30:00\001";
    private static final String INVALID_MESSAGE = "invalid message";
    
    @BeforeEach
    void setUp() {
        protocol = new FixProtocol();
    }
    
    @Test
    @DisplayName("Should return correct protocol name")
    public void shouldReturnCorrectProtocolName() {
        assertEquals("FIX", protocol.getProtocolName());
    }
    
    @Test
    @DisplayName("Should validate FIX messages correctly")
    public void shouldValidateFixMessagesCorrectly() {
        assertTrue(protocol.isValidMessage(VALID_FIX_MESSAGE.getBytes()));
        assertFalse(protocol.isValidMessage(INVALID_MESSAGE.getBytes()));
        assertFalse(protocol.isValidMessage(new byte[0]));
    }
    
    @Test
    @DisplayName("Should parse valid FIX message from bytes")
    public void shouldParseValidFixMessageFromBytes() throws IOException {
        Message message = protocol.parseMessage(VALID_FIX_MESSAGE.getBytes());
        
        assertNotNull(message);
        assertInstanceOf(FixMessage.class, message);
        assertTrue(message.isValid());
        assertEquals("FIX", message.getProtocol());
    }
    
    @Test
    @DisplayName("Should parse valid FIX message from string")
    public void shouldParseValidFixMessageFromString() throws IOException {
        Message message = protocol.parseMessage(VALID_FIX_MESSAGE);
        
        assertNotNull(message);
        assertInstanceOf(FixMessage.class, message);
        assertTrue(message.isValid());
        assertEquals("FIX", message.getProtocol());
    }
    
    @Test
    @DisplayName("Should throw exception for invalid message bytes")
    public void shouldThrowExceptionForInvalidMessageBytes() {
        assertThrows(IOException.class, () -> {
            protocol.parseMessage(INVALID_MESSAGE.getBytes());
        });
    }
    
    @Test
    @DisplayName("Should throw exception for invalid message string")
    public void shouldThrowExceptionForInvalidMessageString() {
        assertThrows(IOException.class, () -> {
            protocol.parseMessage(INVALID_MESSAGE);
        });
    }
    
    @Test
    @DisplayName("Should return supported extensions")
    public void shouldReturnSupportedExtensions() {
        String[] extensions = protocol.getSupportedExtensions();
        
        assertNotNull(extensions);
        List<String> extensionList = Arrays.asList(extensions);
        assertTrue(extensionList.contains("txt"));
        assertTrue(extensionList.contains("fix"));
        assertTrue(extensionList.contains("log"));
    }
    
    @Test
    @DisplayName("Should read messages from file with validation enabled")
    public void shouldReadMessagesFromFileWithValidationEnabled() throws IOException {
        // Create test file
        String fileContent = VALID_FIX_MESSAGE + "\n" +
                            "8=FIX.4.2\0019=40\00135=A\00149=SENDER\00156=TARGET\001\n" +
                            INVALID_MESSAGE + "\n" +
                            "\n"; // Empty line
        
        Path testFile = tempDir.resolve("test-messages.fix");
        Files.write(testFile, fileContent.getBytes());
        
        // Create config with validation enabled
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return true; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return 8; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        protocol.readMessagesFromFile(testFile, messages::add, config);
        
        assertEquals(2, messages.size()); // Only valid messages should be processed
        assertTrue(messages.get(0).isValid());
        assertTrue(messages.get(1).isValid());
    }
    
    @Test
    @DisplayName("Should read messages from file with validation disabled")
    public void shouldReadMessagesFromFileWithValidationDisabled() throws IOException {
        // Create test file with mixed valid and invalid content
        String fileContent = VALID_FIX_MESSAGE + "\n" +
                            "8=FIX.4.2\0019=40\00135=A\00149=SENDER\00156=TARGET\001\n" +
                            INVALID_MESSAGE + "\n";
        
        Path testFile = tempDir.resolve("test-messages.fix");
        Files.write(testFile, fileContent.getBytes());
        
        // Create config with validation disabled
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return false; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return 8; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        protocol.readMessagesFromFile(testFile, messages::add, config);
        
        assertEquals(2, messages.size()); // Only valid FIX format messages should be processed
    }
    
    @Test
    @DisplayName("Should handle empty file gracefully")
    public void shouldHandleEmptyFileGracefully() throws IOException {
        Path emptyFile = tempDir.resolve("empty.fix");
        Files.write(emptyFile, "".getBytes());
        
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return true; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return 8; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        protocol.readMessagesFromFile(emptyFile, messages::add, config);
        
        assertEquals(0, messages.size());
    }
    
    @Test
    @DisplayName("Should handle file with only whitespace gracefully")
    public void shouldHandleFileWithOnlyWhitespaceGracefully() throws IOException {
        Path whitespaceFile = tempDir.resolve("whitespace.fix");
        Files.write(whitespaceFile, "   \n\n  \t  \n".getBytes());
        
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return true; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return 8; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        protocol.readMessagesFromFile(whitespaceFile, messages::add, config);
        
        assertEquals(0, messages.size());
    }
    
    @Test
    @DisplayName("Should use custom buffer size from config")
    public void shouldUseCustomBufferSizeFromConfig() throws IOException {
        String fileContent = VALID_FIX_MESSAGE + "\n";
        Path testFile = tempDir.resolve("test.fix");
        Files.write(testFile, fileContent.getBytes());
        
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return false; }
            @Override
            public int getBufferSize() { return 1024; }
            @Override
            public int getHeaderLength() { return 8; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        
        // Should not throw exception even with small buffer size
        assertDoesNotThrow(() -> {
            protocol.readMessagesFromFile(testFile, messages::add, config);
        });
        
        assertEquals(1, messages.size());
    }
    
    @Test
    @DisplayName("Should handle non-existent file appropriately")
    public void shouldHandleNonExistentFileAppropriately() {
        Path nonExistentFile = tempDir.resolve("non-existent.fix");
        
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return true; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return 8; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        
        assertThrows(IOException.class, () -> {
            protocol.readMessagesFromFile(nonExistentFile, messages::add, config);
        });
    }
}