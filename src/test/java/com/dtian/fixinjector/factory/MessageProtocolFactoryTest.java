package com.dtian.fixinjector.factory;

import com.dtian.fixinjector.protocol.MessageProtocol;
import com.dtian.fixinjector.protocol.impl.ByteHeaderXmlProtocol;
import com.dtian.fixinjector.protocol.impl.FixProtocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class MessageProtocolFactoryTest {
    
    @Test
    @DisplayName("Should create FIX protocol instance")
    public void shouldCreateFixProtocolInstance() {
        MessageProtocol protocol = MessageProtocolFactory.createProtocol("FIX");
        
        assertNotNull(protocol);
        assertInstanceOf(FixProtocol.class, protocol);
        assertEquals("FIX", protocol.getProtocolName());
    }
    
    @Test
    @DisplayName("Should create ByteHeaderXML protocol instance")
    public void shouldCreateByteHeaderXmlProtocolInstance() {
        MessageProtocol protocol = MessageProtocolFactory.createProtocol("BYTE_HEADER_XML");
        
        assertNotNull(protocol);
        assertInstanceOf(ByteHeaderXmlProtocol.class, protocol);
        assertEquals("BYTE_HEADER_XML", protocol.getProtocolName());
    }
    
    @Test
    @DisplayName("Should be case insensitive when creating protocols")
    public void shouldBeCaseInsensitiveWhenCreatingProtocols() {
        MessageProtocol fixLower = MessageProtocolFactory.createProtocol("fix");
        MessageProtocol fixUpper = MessageProtocolFactory.createProtocol("FIX");
        MessageProtocol fixMixed = MessageProtocolFactory.createProtocol("Fix");
        
        assertInstanceOf(FixProtocol.class, fixLower);
        assertInstanceOf(FixProtocol.class, fixUpper);
        assertInstanceOf(FixProtocol.class, fixMixed);
        
        MessageProtocol xmlLower = MessageProtocolFactory.createProtocol("byte_header_xml");
        MessageProtocol xmlUpper = MessageProtocolFactory.createProtocol("BYTE_HEADER_XML");
        
        assertInstanceOf(ByteHeaderXmlProtocol.class, xmlLower);
        assertInstanceOf(ByteHeaderXmlProtocol.class, xmlUpper);
    }
    
    @Test
    @DisplayName("Should throw exception for unknown protocol")
    public void shouldThrowExceptionForUnknownProtocol() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageProtocolFactory.createProtocol("UNKNOWN_PROTOCOL");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            MessageProtocolFactory.createProtocol("INVALID");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            MessageProtocolFactory.createProtocol("");
        });
    }
    
    @Test
    @DisplayName("Should return all available protocols")
    public void shouldReturnAllAvailableProtocols() {
        String[] availableProtocols = MessageProtocolFactory.getAvailableProtocols();
        
        assertNotNull(availableProtocols);
        assertTrue(availableProtocols.length >= 2);
        
        List<String> protocolList = Arrays.asList(availableProtocols);
        assertTrue(protocolList.contains("FIX"));
        assertTrue(protocolList.contains("BYTE_HEADER_XML"));
    }
    
    @Test
    @DisplayName("Should check if protocol is supported")
    public void shouldCheckIfProtocolIsSupported() {
        assertTrue(MessageProtocolFactory.isProtocolSupported("FIX"));
        assertTrue(MessageProtocolFactory.isProtocolSupported("BYTE_HEADER_XML"));
        assertTrue(MessageProtocolFactory.isProtocolSupported("fix")); // Case insensitive
        assertTrue(MessageProtocolFactory.isProtocolSupported("byte_header_xml")); // Case insensitive
        
        assertFalse(MessageProtocolFactory.isProtocolSupported("UNKNOWN"));
        assertFalse(MessageProtocolFactory.isProtocolSupported("INVALID"));
        assertFalse(MessageProtocolFactory.isProtocolSupported(""));
    }
    
    @Test
    @DisplayName("Should allow registering new protocols")
    public void shouldAllowRegisteringNewProtocols() {
        // Create a mock protocol for testing
        MessageProtocol mockProtocol = new MessageProtocol() {
            @Override
            public String getProtocolName() { return "TEST_PROTOCOL"; }
            
            @Override
            public boolean isValidMessage(byte[] data) { return true; }
            
            @Override
            public com.dtian.fixinjector.model.Message parseMessage(byte[] data) { return null; }
            
            @Override
            public void readMessagesFromFile(java.nio.file.Path filePath, 
                    java.util.function.Consumer<com.dtian.fixinjector.model.Message> messageConsumer, 
                    com.dtian.fixinjector.protocol.MessageReaderConfig config) { }
            
            @Override
            public String[] getSupportedExtensions() { return new String[]{"test"}; }
        };
        
        // Register the mock protocol
        MessageProtocolFactory.registerProtocol(mockProtocol);
        
        // Verify it's registered
        assertTrue(MessageProtocolFactory.isProtocolSupported("TEST_PROTOCOL"));
        
        MessageProtocol retrievedProtocol = MessageProtocolFactory.createProtocol("TEST_PROTOCOL");
        assertNotNull(retrievedProtocol);
        assertEquals("TEST_PROTOCOL", retrievedProtocol.getProtocolName());
        
        // Verify it appears in available protocols
        String[] availableProtocols = MessageProtocolFactory.getAvailableProtocols();
        List<String> protocolList = Arrays.asList(availableProtocols);
        assertTrue(protocolList.contains("TEST_PROTOCOL"));
    }
    
    @Test
    @DisplayName("Should handle protocol registration case insensitively")
    public void shouldHandleProtocolRegistrationCaseInsensitively() {
        MessageProtocol mockProtocol = new MessageProtocol() {
            @Override
            public String getProtocolName() { return "CaSe_TeSt"; }
            
            @Override
            public boolean isValidMessage(byte[] data) { return true; }
            
            @Override
            public com.dtian.fixinjector.model.Message parseMessage(byte[] data) { return null; }
            
            @Override
            public void readMessagesFromFile(java.nio.file.Path filePath, 
                    java.util.function.Consumer<com.dtian.fixinjector.model.Message> messageConsumer, 
                    com.dtian.fixinjector.protocol.MessageReaderConfig config) { }
            
            @Override
            public String[] getSupportedExtensions() { return new String[]{"case"}; }
        };
        
        MessageProtocolFactory.registerProtocol(mockProtocol);
        
        // Should be able to retrieve using different cases
        assertTrue(MessageProtocolFactory.isProtocolSupported("CASE_TEST"));
        assertTrue(MessageProtocolFactory.isProtocolSupported("case_test"));
        assertTrue(MessageProtocolFactory.isProtocolSupported("CaSe_TeSt"));
        
        MessageProtocol retrieved1 = MessageProtocolFactory.createProtocol("CASE_TEST");
        MessageProtocol retrieved2 = MessageProtocolFactory.createProtocol("case_test");
        
        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
        assertEquals("CaSe_TeSt", retrieved1.getProtocolName());
        assertEquals("CaSe_TeSt", retrieved2.getProtocolName());
    }
    
    @Test
    @DisplayName("Should maintain registry integrity after multiple operations")
    public void shouldMaintainRegistryIntegrityAfterMultipleOperations() {
        // Get initial count
        int initialCount = MessageProtocolFactory.getAvailableProtocols().length;
        
        // Register a new protocol
        MessageProtocol testProtocol = new MessageProtocol() {
            @Override
            public String getProtocolName() { return "INTEGRITY_TEST"; }
            @Override
            public boolean isValidMessage(byte[] data) { return true; }
            @Override
            public com.dtian.fixinjector.model.Message parseMessage(byte[] data) { return null; }
            @Override
            public void readMessagesFromFile(java.nio.file.Path filePath, 
                    java.util.function.Consumer<com.dtian.fixinjector.model.Message> messageConsumer, 
                    com.dtian.fixinjector.protocol.MessageReaderConfig config) { }
            @Override
            public String[] getSupportedExtensions() { return new String[]{"integrity"}; }
        };
        
        MessageProtocolFactory.registerProtocol(testProtocol);
        
        // Verify count increased
        assertEquals(initialCount + 1, MessageProtocolFactory.getAvailableProtocols().length);
        
        // Verify original protocols still work
        assertNotNull(MessageProtocolFactory.createProtocol("FIX"));
        assertNotNull(MessageProtocolFactory.createProtocol("BYTE_HEADER_XML"));
        
        // Verify new protocol works
        assertNotNull(MessageProtocolFactory.createProtocol("INTEGRITY_TEST"));
        
        // Verify all supported checks still work
        assertTrue(MessageProtocolFactory.isProtocolSupported("FIX"));
        assertTrue(MessageProtocolFactory.isProtocolSupported("BYTE_HEADER_XML"));
        assertTrue(MessageProtocolFactory.isProtocolSupported("INTEGRITY_TEST"));
    }
}