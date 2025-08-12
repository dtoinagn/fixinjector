package com.dtian.fixinjector;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FixMessage {
    private static final char SOH = '\001';
    private final String rawMessage;
    private final byte[] messageBytes;
    private final Map<Integer, String> tags;
    private final int length;

    public FixMessage(String rawMessage) {
        this.rawMessage = rawMessage;
        this.messageBytes = rawMessage.getBytes();
        this.length = messageBytes.length;
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

    public byte[] getMessageBytes() {
        return messageBytes.clone();
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(messageBytes);
    }

    public int getLength() {
        return length;
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

    public boolean isValid() {
        return hasTag(8) && hasTag(9) && hasTag(35) && hasTag(49) && hasTag(56);
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
                ", length=" + length +
                '}';
    }

    public static boolean isValidFixMessage(String message) {
        return message != null && 
               message.contains("8=") && 
               message.contains("35=") && 
               message.contains(String.valueOf(SOH));
    }
}