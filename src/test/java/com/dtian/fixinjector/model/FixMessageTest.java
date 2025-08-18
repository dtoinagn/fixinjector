package com.dtian.fixinjector.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class FixMessageTest {
    
    private static final String VALID_FIX_MESSAGE = "8=FIX.4.2\0019=154\00135=D\00149=SENDER\00156=TARGET\00134=1\00152=20231201-10:30:00\001";
    private static final String MINIMAL_FIX_MESSAGE = "8=FIX.4.2\0019=40\00135=A\00149=SENDER\00156=TARGET\001";
    private static final String INVALID_FIX_MESSAGE = "invalid message";
    
    @Test
    @DisplayName("Should parse valid FIX message correctly")
    public void shouldParseValidFixMessage() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        
        assertEquals("FIX.4.2", message.getBeginString());
        assertEquals("154", message.getBodyLength());
        assertEquals("D", message.getMsgType());
        assertEquals("SENDER", message.getSenderCompID());
        assertEquals("TARGET", message.getTargetCompID());
        assertEquals("1", message.getMsgSeqNum());
        assertEquals("20231201-10:30:00", message.getSendingTime());
        assertTrue(message.isValid());
        assertEquals("FIX", message.getProtocol());
    }
    
    @Test
    @DisplayName("Should validate minimal FIX message")
    public void shouldValidateMinimalFixMessage() {
        FixMessage message = new FixMessage(MINIMAL_FIX_MESSAGE);
        
        assertTrue(message.isValid());
        assertEquals("FIX.4.2", message.getBeginString());
        assertEquals("A", message.getMsgType());
        assertEquals("SENDER", message.getSenderCompID());
        assertEquals("TARGET", message.getTargetCompID());
    }
    
    @Test
    @DisplayName("Should handle invalid FIX message gracefully")
    public void shouldHandleInvalidFixMessage() {
        FixMessage message = new FixMessage(INVALID_FIX_MESSAGE);
        
        assertFalse(message.isValid());
        assertNull(message.getMsgType());
        assertNull(message.getSenderCompID());
        assertNull(message.getTargetCompID());
    }
    
    @Test
    @DisplayName("Should check for tag existence")
    public void shouldCheckTagExistence() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        
        assertTrue(message.hasTag(8));  // BeginString
        assertTrue(message.hasTag(35)); // MsgType
        assertTrue(message.hasTag(49)); // SenderCompID
        assertFalse(message.hasTag(999)); // Non-existent tag
    }
    
    @Test
    @DisplayName("Should return correct tag values")
    public void shouldReturnCorrectTagValues() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        
        assertEquals("FIX.4.2", message.getTag(8));
        assertEquals("D", message.getTag(35));
        assertEquals("SENDER", message.getTag(49));
        assertNull(message.getTag(999));
    }
    
    @Test
    @DisplayName("Should handle empty tags gracefully")
    public void shouldHandleEmptyTagsGracefully() {
        String messageWithEmptyTag = "8=FIX.4.2\00135=D\001=\00149=SENDER\00156=TARGET\001";
        FixMessage message = new FixMessage(messageWithEmptyTag);
        
        assertEquals("FIX.4.2", message.getBeginString());
        assertEquals("D", message.getMsgType());
    }
    
    @Test
    @DisplayName("Should handle malformed tags gracefully")
    public void shouldHandleMalformedTagsGracefully() {
        String messageWithBadTag = "8=FIX.4.2\00135=D\001ABC=value\00149=SENDER\00156=TARGET\001";
        FixMessage message = new FixMessage(messageWithBadTag);
        
        assertEquals("FIX.4.2", message.getBeginString());
        assertEquals("D", message.getMsgType());
        assertFalse(message.hasTag(-1));
    }
    
    @Test
    @DisplayName("Should return all tags as map")
    public void shouldReturnAllTagsAsMap() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        var allTags = message.getAllTags();
        
        assertNotNull(allTags);
        assertTrue(allTags.containsKey(8));
        assertTrue(allTags.containsKey(35));
        assertEquals("FIX.4.2", allTags.get(8));
        assertEquals("D", allTags.get(35));
    }
    
    @Test
    @DisplayName("Should return defensive copy of message bytes")
    public void shouldReturnDefensiveCopyOfMessageBytes() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        byte[] bytes1 = message.getMessageBytes();
        byte[] bytes2 = message.getMessageBytes();
        
        assertNotSame(bytes1, bytes2);
        assertArrayEquals(bytes1, bytes2);
        
        // Modifying returned bytes should not affect the message
        bytes1[0] = (byte) 'X';
        assertNotEquals(bytes1[0], message.getMessageBytes()[0]);
    }
    
    @Test
    @DisplayName("Should validate FIX message format statically")
    public void shouldValidateFixMessageFormatStatically() {
        assertTrue(FixMessage.isValidFixMessage(VALID_FIX_MESSAGE));
        assertTrue(FixMessage.isValidFixMessage(MINIMAL_FIX_MESSAGE));
        assertFalse(FixMessage.isValidFixMessage(INVALID_FIX_MESSAGE));
        assertFalse(FixMessage.isValidFixMessage(null));
        assertFalse(FixMessage.isValidFixMessage(""));
        assertFalse(FixMessage.isValidFixMessage("8=FIX.4.2")); // Missing SOH and required fields
    }
    
    @Test
    @DisplayName("Should have meaningful toString representation")
    public void shouldHaveMeaningfulToStringRepresentation() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        String toString = message.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("FixMessage"));
        assertTrue(toString.contains("msgType=D"));
        assertTrue(toString.contains("sender=SENDER"));
        assertTrue(toString.contains("target=TARGET"));
    }
    
    @Test
    @DisplayName("Should get correct message length")
    public void shouldGetCorrectMessageLength() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        assertEquals(VALID_FIX_MESSAGE.getBytes().length, message.getLength());
    }
    
    @Test
    @DisplayName("Should return correct ByteBuffer")
    public void shouldReturnCorrectByteBuffer() {
        FixMessage message = new FixMessage(VALID_FIX_MESSAGE);
        var byteBuffer = message.getByteBuffer();
        
        assertNotNull(byteBuffer);
        assertEquals(VALID_FIX_MESSAGE.getBytes().length, byteBuffer.remaining());
    }
}