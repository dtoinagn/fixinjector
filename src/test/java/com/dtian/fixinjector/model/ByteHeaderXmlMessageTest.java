package com.dtian.fixinjector.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class ByteHeaderXmlMessageTest {
    
    private static final byte[] TEST_HEADER = {0x01, 0x02, 0x03, 0x04};
    private static final String TEST_XML = "<message><type>ORDER</type><id>123</id></message>";
    private static final String SIMPLE_XML = "<test>data</test>";
    
    @Test
    @DisplayName("Should create message from header and XML payload")
    public void shouldCreateMessageFromHeaderAndXml() {
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, TEST_XML);
        
        assertArrayEquals(TEST_HEADER, message.getHeader());
        assertEquals(TEST_XML, message.getXmlPayload());
        assertEquals(TEST_HEADER.length, message.getHeaderLength());
        assertEquals(TEST_HEADER.length + TEST_XML.getBytes().length, message.getLength());
        assertEquals("BYTE_HEADER_XML", message.getProtocol());
        assertEquals("XML", message.getMessageType());
        assertTrue(message.isValid());
    }
    
    @Test
    @DisplayName("Should create message from full byte array with header length")
    public void shouldCreateMessageFromFullByteArray() {
        byte[] xmlBytes = TEST_XML.getBytes();
        byte[] fullMessage = new byte[TEST_HEADER.length + xmlBytes.length];
        System.arraycopy(TEST_HEADER, 0, fullMessage, 0, TEST_HEADER.length);
        System.arraycopy(xmlBytes, 0, fullMessage, TEST_HEADER.length, xmlBytes.length);
        
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(fullMessage, TEST_HEADER.length);
        
        assertArrayEquals(TEST_HEADER, message.getHeader());
        assertEquals(TEST_XML, message.getXmlPayload());
        assertEquals(TEST_HEADER.length, message.getHeaderLength());
        assertEquals(fullMessage.length, message.getLength());
    }
    
    @Test
    @DisplayName("Should handle empty XML payload")
    public void shouldHandleEmptyXmlPayload() {
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, "");
        
        assertArrayEquals(TEST_HEADER, message.getHeader());
        assertEquals("", message.getXmlPayload());
        assertEquals(TEST_HEADER.length, message.getLength());
        assertTrue(message.isValid());
    }
    
    @Test
    @DisplayName("Should handle empty header")
    public void shouldHandleEmptyHeader() {
        byte[] emptyHeader = new byte[0];
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(emptyHeader, SIMPLE_XML);
        
        assertEquals(0, message.getHeaderLength());
        assertEquals(SIMPLE_XML, message.getXmlPayload());
        assertEquals(SIMPLE_XML.getBytes().length, message.getLength());
    }
    
    @Test
    @DisplayName("Should return defensive copy of header")
    public void shouldReturnDefensiveCopyOfHeader() {
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, TEST_XML);
        byte[] header1 = message.getHeader();
        byte[] header2 = message.getHeader();
        
        assertNotSame(header1, header2);
        assertArrayEquals(header1, header2);
        
        // Modifying returned header should not affect the message
        header1[0] = (byte) 0xFF;
        assertNotEquals(header1[0], message.getHeader()[0]);
    }
    
    @Test
    @DisplayName("Should return defensive copy of message bytes")
    public void shouldReturnDefensiveCopyOfMessageBytes() {
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, TEST_XML);
        byte[] bytes1 = message.getMessageBytes();
        byte[] bytes2 = message.getMessageBytes();
        
        assertNotSame(bytes1, bytes2);
        assertArrayEquals(bytes1, bytes2);
        
        // Modifying returned bytes should not affect the message
        bytes1[0] = (byte) 0xFF;
        assertNotEquals(bytes1[0], message.getMessageBytes()[0]);
    }
    
    @Test
    @DisplayName("Should validate message format statically")
    public void shouldValidateMessageFormatStatically() {
        byte[] validMessage = new byte[10];
        byte[] tooShortMessage = new byte[2];
        byte[] exactHeaderLength = new byte[4];
        
        assertTrue(ByteHeaderXmlMessage.isValidMessage(validMessage, 4));
        assertFalse(ByteHeaderXmlMessage.isValidMessage(tooShortMessage, 4));
        assertFalse(ByteHeaderXmlMessage.isValidMessage(exactHeaderLength, 4));
        assertFalse(ByteHeaderXmlMessage.isValidMessage(null, 4));
    }
    
    @Test
    @DisplayName("Should handle various header lengths")
    public void shouldHandleVariousHeaderLengths() {
        byte[] header1 = {0x01};
        byte[] header8 = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        byte[] header16 = new byte[16];
        
        ByteHeaderXmlMessage msg1 = new ByteHeaderXmlMessage(header1, SIMPLE_XML);
        ByteHeaderXmlMessage msg8 = new ByteHeaderXmlMessage(header8, SIMPLE_XML);
        ByteHeaderXmlMessage msg16 = new ByteHeaderXmlMessage(header16, SIMPLE_XML);
        
        assertEquals(1, msg1.getHeaderLength());
        assertEquals(8, msg8.getHeaderLength());
        assertEquals(16, msg16.getHeaderLength());
    }
    
    @Test
    @DisplayName("Should have meaningful toString representation")
    public void shouldHaveMeaningfulToStringRepresentation() {
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, TEST_XML);
        String toString = message.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("ByteHeaderXmlMessage"));
        assertTrue(toString.contains("headerLength=" + TEST_HEADER.length));
        assertTrue(toString.contains("xmlLength=" + TEST_XML.length()));
        assertTrue(toString.contains("totalLength=" + message.getLength()));
    }
    
    @Test
    @DisplayName("Should handle Unicode in XML payload")
    public void shouldHandleUnicodeInXmlPayload() {
        String unicodeXml = "<message>Test with unicode: αβγ 中文 🚀</message>";
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, unicodeXml);
        
        assertEquals(unicodeXml, message.getXmlPayload());
        assertTrue(message.getLength() > TEST_HEADER.length + unicodeXml.length()); // UTF-8 encoding
    }
    
    @Test
    @DisplayName("Should return correct ByteBuffer")
    public void shouldReturnCorrectByteBuffer() {
        ByteHeaderXmlMessage message = new ByteHeaderXmlMessage(TEST_HEADER, TEST_XML);
        var byteBuffer = message.getByteBuffer();
        
        assertNotNull(byteBuffer);
        assertEquals(message.getLength(), byteBuffer.remaining());
        
        // Verify the buffer contains header + XML
        byte[] bufferContent = new byte[byteBuffer.remaining()];
        byteBuffer.get(bufferContent);
        assertArrayEquals(message.getMessageBytes(), bufferContent);
    }
}