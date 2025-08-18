package com.dtian.fixinjector.protocol.impl;

import com.dtian.fixinjector.model.ByteHeaderXmlMessage;
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

public class ByteHeaderXmlProtocolTest {
    
    private ByteHeaderXmlProtocol protocol;
    
    @TempDir
    Path tempDir;
    
    private static final String TEST_XML = "<message><type>ORDER</type><id>123</id></message>";
    private static final byte[] TEST_HEADER = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    
    @BeforeEach
    void setUp() {
        protocol = new ByteHeaderXmlProtocol();
    }
    
    @Test
    @DisplayName("Should return correct protocol name")
    public void shouldReturnCorrectProtocolName() {
        assertEquals("BYTE_HEADER_XML", protocol.getProtocolName());
    }
    
    @Test
    @DisplayName("Should validate messages correctly")
    public void shouldValidateMessagesCorrectly() {
        assertTrue(protocol.isValidMessage(new byte[]{0x01, 0x02}));
        assertTrue(protocol.isValidMessage(TEST_HEADER));
        assertFalse(protocol.isValidMessage(null));
        assertFalse(protocol.isValidMessage(new byte[0]));
    }
    
    @Test
    @DisplayName("Should parse message from bytes")
    public void shouldParseMessageFromBytes() throws IOException {
        byte[] testData = "test data".getBytes();
        Message message = protocol.parseMessage(testData);
        
        assertNotNull(message);
        assertInstanceOf(ByteHeaderXmlMessage.class, message);
        assertEquals("BYTE_HEADER_XML", message.getProtocol());
        assertTrue(message.isValid());
    }
    
    @Test
    @DisplayName("Should parse message from string")
    public void shouldParseMessageFromString() throws IOException {
        Message message = protocol.parseMessage(TEST_XML);
        
        assertNotNull(message);
        assertInstanceOf(ByteHeaderXmlMessage.class, message);
        assertEquals("BYTE_HEADER_XML", message.getProtocol());
        
        ByteHeaderXmlMessage xmlMessage = (ByteHeaderXmlMessage) message;
        assertEquals(TEST_XML, xmlMessage.getXmlPayload());
        assertEquals(8, xmlMessage.getHeaderLength()); // Default header length
    }
    
    @Test
    @DisplayName("Should throw exception for invalid byte data")
    public void shouldThrowExceptionForInvalidByteData() {
        assertThrows(IOException.class, () -> {
            protocol.parseMessage((byte[]) null);
        });
        
        assertThrows(IOException.class, () -> {
            protocol.parseMessage(new byte[0]);
        });
    }
    
    @Test
    @DisplayName("Should return supported extensions")
    public void shouldReturnSupportedExtensions() {
        String[] extensions = protocol.getSupportedExtensions();
        
        assertNotNull(extensions);
        List<String> extensionList = Arrays.asList(extensions);
        assertTrue(extensionList.contains("xml"));
        assertTrue(extensionList.contains("bin"));
        assertTrue(extensionList.contains("dat"));
    }
    
    @Test
    @DisplayName("Should read XML file as text")
    public void shouldReadXmlFileAsText() throws IOException {
        String xmlContent = TEST_XML + "\n<another>message</another>\n";
        Path xmlFile = tempDir.resolve("test.xml");
        Files.write(xmlFile, xmlContent.getBytes());
        
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
        protocol.readMessagesFromFile(xmlFile, messages::add, config);
        
        assertEquals(2, messages.size());
        assertTrue(messages.get(0) instanceof ByteHeaderXmlMessage);
        assertTrue(messages.get(1) instanceof ByteHeaderXmlMessage);
    }
    
    @Test
    @DisplayName("Should read TXT file as text")
    public void shouldReadTxtFileAsText() throws IOException {
        String textContent = "<data>line1</data>\n<data>line2</data>\n";
        Path txtFile = tempDir.resolve("test.txt");
        Files.write(txtFile, textContent.getBytes());
        
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
        protocol.readMessagesFromFile(txtFile, messages::add, config);
        
        assertEquals(2, messages.size());
    }
    
    @Test
    @DisplayName("Should read binary file")
    public void shouldReadBinaryFile() throws IOException {
        // Create test binary data with header + XML
        byte[] xmlBytes = TEST_XML.getBytes();
        byte[] binaryData = new byte[TEST_HEADER.length + xmlBytes.length];
        System.arraycopy(TEST_HEADER, 0, binaryData, 0, TEST_HEADER.length);
        System.arraycopy(xmlBytes, 0, binaryData, TEST_HEADER.length, xmlBytes.length);
        
        Path binFile = tempDir.resolve("test.bin");
        Files.write(binFile, binaryData);
        
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return false; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return TEST_HEADER.length; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        protocol.readMessagesFromFile(binFile, messages::add, config);
        
        assertEquals(1, messages.size());
        ByteHeaderXmlMessage message = (ByteHeaderXmlMessage) messages.get(0);
        assertArrayEquals(TEST_HEADER, message.getHeader());
        assertEquals(TEST_XML, message.getXmlPayload());
    }
    
    @Test
    @DisplayName("Should handle empty XML file")
    public void shouldHandleEmptyXmlFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.xml");
        Files.write(emptyFile, "".getBytes());
        
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
        protocol.readMessagesFromFile(emptyFile, messages::add, config);
        
        assertEquals(0, messages.size());
    }
    
    @Test
    @DisplayName("Should handle validation enabled")
    public void shouldHandleValidationEnabled() throws IOException {
        String xmlContent = TEST_XML + "\n<another>message</another>\n";
        Path xmlFile = tempDir.resolve("test.xml");
        Files.write(xmlFile, xmlContent.getBytes());
        
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
        protocol.readMessagesFromFile(xmlFile, messages::add, config);
        
        // All ByteHeaderXmlMessage instances are valid by default
        assertEquals(2, messages.size());
        assertTrue(messages.get(0).isValid());
        assertTrue(messages.get(1).isValid());
    }
    
    @Test
    @DisplayName("Should handle different header lengths")
    public void shouldHandleDifferentHeaderLengths() throws IOException {
        byte[] data16 = new byte[32]; // 16 byte header + 16 byte payload
        Arrays.fill(data16, 0, 16, (byte) 0xAA); // Header
        Arrays.fill(data16, 16, 32, (byte) 0x55); // Payload
        
        Path binFile = tempDir.resolve("test16.bin");
        Files.write(binFile, data16);
        
        MessageReaderConfig config = new MessageReaderConfig() {
            @Override
            public boolean isValidationEnabled() { return false; }
            @Override
            public int getBufferSize() { return 8192; }
            @Override
            public int getHeaderLength() { return 16; }
            @Override
            public boolean isRecursiveEnabled() { return false; }
            @Override
            public int getMaxRecursionDepth() { return 10; }
        };
        
        List<Message> messages = new ArrayList<>();
        protocol.readMessagesFromFile(binFile, messages::add, config);
        
        assertEquals(1, messages.size());
        ByteHeaderXmlMessage message = (ByteHeaderXmlMessage) messages.get(0);
        assertEquals(16, message.getHeaderLength());
        assertEquals(16, message.getHeader().length);
    }
    
    @Test
    @DisplayName("Should handle non-existent file")
    public void shouldHandleNonExistentFile() {
        Path nonExistentFile = tempDir.resolve("non-existent.xml");
        
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
        
        assertThrows(IOException.class, () -> {
            protocol.readMessagesFromFile(nonExistentFile, messages::add, config);
        });
    }
}