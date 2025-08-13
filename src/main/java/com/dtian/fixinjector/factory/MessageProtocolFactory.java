package com.dtian.fixinjector.factory;

import com.dtian.fixinjector.protocol.MessageProtocol;
import com.dtian.fixinjector.protocol.impl.ByteHeaderXmlProtocol;
import com.dtian.fixinjector.protocol.impl.FixProtocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating message protocol instances.
 * Uses registry pattern to allow easy extension with new protocols.
 */
public class MessageProtocolFactory {
    private static final Map<String, MessageProtocol> protocolRegistry = new HashMap<>();
    
    static {
        registerProtocol(new FixProtocol());
        registerProtocol(new ByteHeaderXmlProtocol());
    }
    
    /**
     * Register a new protocol implementation
     */
    public static void registerProtocol(MessageProtocol protocol) {
        protocolRegistry.put(protocol.getProtocolName().toUpperCase(), protocol);
    }
    
    /**
     * Create a protocol instance by name
     */
    public static MessageProtocol createProtocol(String protocolName) {
        MessageProtocol protocol = protocolRegistry.get(protocolName.toUpperCase());
        if (protocol == null) {
            throw new IllegalArgumentException("Unknown protocol: " + protocolName);
        }
        return protocol;
    }
    
    /**
     * Get all registered protocol names
     */
    public static String[] getAvailableProtocols() {
        return protocolRegistry.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if a protocol is supported
     */
    public static boolean isProtocolSupported(String protocolName) {
        return protocolRegistry.containsKey(protocolName.toUpperCase());
    }
}