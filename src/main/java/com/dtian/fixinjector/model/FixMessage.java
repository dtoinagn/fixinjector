package com.dtian.fixinjector.model;

import java.util.HashMap;
import java.util.Map;

/**
 * FIX protocol message implementation
 */
public class FixMessage extends Message {
    private static final char SOH = '\001';
    private final String rawMessage;
    private final Map<Integer, String> tags;

    public FixMessage(String rawMessage) {
        super(rawMessage.getBytes());
        this.rawMessage = rawMessage;
        this.tags = new HashMap<>();
        parseMessage();
    }

    private void parseMessage() {
        String[] fields = rawMessage.split(String.valueOf(SOH));
        for (String field : fields) {
            if (field.isEmpty()) continue;
            
            int equalIndex = field.indexOf('=');
            if (equalIndex > 0 && equalIndex < field.length() - 1) {
                try {
                    int tag = Integer.parseInt(field.substring(0, equalIndex));
                    String value = field.substring(equalIndex + 1);
                    tags.put(tag, value);
                } catch (NumberFormatException e) {
                    // Skip invalid tags
                }
            }
        }
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getTag(int tagNumber) {
        return tags.get(tagNumber);
    }

    public boolean hasTag(int tagNumber) {
        return tags.containsKey(tagNumber);
    }

    public String getMsgType() {
        return getTag(35);
    }

    public String getSenderCompID() {
        return getTag(49);
    }

    public String getTargetCompID() {
        return getTag(56);
    }

    public String getMsgSeqNum() {
        return getTag(34);
    }

    public String getSendingTime() {
        return getTag(52);
    }

    @Override
    public boolean isValid() {
        return hasTag(8) && hasTag(9) && hasTag(35) && hasTag(49) && hasTag(56);
    }

    @Override
    public String getMessageType() {
        return getMsgType();
    }

    @Override
    public String getProtocol() {
        return "FIX";
    }

    public String getBeginString() {
        return getTag(8);
    }

    public String getBodyLength() {
        return getTag(9);
    }

    public Map<Integer, String> getAllTags() {
        return new HashMap<>(tags);
    }

    @Override
    public String toString() {
        return "FixMessage{" +
                "msgType=" + getMsgType() +
                ", sender=" + getSenderCompID() +
                ", target=" + getTargetCompID() +
                ", seqNum=" + getMsgSeqNum() +
                ", length=" + getLength() +
                '}';
    }

    public static boolean isValidFixMessage(String message) {
        return message != null && 
               message.contains("8=") && 
               message.contains("35=") && 
               message.contains(String.valueOf(SOH));
    }
}