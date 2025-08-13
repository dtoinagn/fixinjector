package com.dtian.fixinjector.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Message implementation for protocol with byte header and XML payload.
 * Format: [byte header][XML payload]
 */
public class ByteHeaderXmlMessage extends Message {
    private final byte[] header;
    private final String xmlPayload;
    private final int headerLength;

    public ByteHeaderXmlMessage(byte[] header, String xmlPayload) {
        super(createMessageBytes(header, xmlPayload));
        this.header = header.clone();
        this.xmlPayload = xmlPayload;
        this.headerLength = header.length;
    }

    public ByteHeaderXmlMessage(byte[] fullMessageBytes, int headerLength) {
        super(fullMessageBytes);
        this.headerLength = headerLength;
        this.header = Arrays.copyOfRange(fullMessageBytes, 0, headerLength);
        
        byte[] xmlBytes = Arrays.copyOfRange(fullMessageBytes, headerLength, fullMessageBytes.length);
        this.xmlPayload = new String(xmlBytes, StandardCharsets.UTF_8);
    }

    private static byte[] createMessageBytes(byte[] header, String xmlPayload) {
        byte[] xmlBytes = xmlPayload.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[header.length + xmlBytes.length];
        
        System.arraycopy(header, 0, combined, 0, header.length);
        System.arraycopy(xmlBytes, 0, combined, header.length, xmlBytes.length);
        
        return combined;
    }

    public byte[] getHeader() {
        return header.clone();
    }

    public String getXmlPayload() {
        return xmlPayload;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    @Override
    public boolean isValid() {
        // No validation required as per requirements
        return true;
    }

    @Override
    public String getMessageType() {
        // Extract message type from XML if needed, or return generic type
        return "XML";
    }

    @Override
    public String getProtocol() {
        return "BYTE_HEADER_XML";
    }

    @Override
    public String toString() {
        return "ByteHeaderXmlMessage{" +
                "headerLength=" + headerLength +
                ", xmlLength=" + (xmlPayload != null ? xmlPayload.length() : 0) +
                ", totalLength=" + length +
                '}';
    }

    /**
     * Check if the given bytes could be a valid byte header + XML message
     */
    public static boolean isValidMessage(byte[] messageBytes, int headerLength) {
        if (messageBytes == null || messageBytes.length <= headerLength) {
            return false;
        }
        
        // Basic check: ensure we have header + some payload
        return messageBytes.length > headerLength;
    }
}