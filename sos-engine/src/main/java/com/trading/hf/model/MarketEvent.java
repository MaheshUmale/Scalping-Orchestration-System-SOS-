package com.trading.hf.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.trading.hf.patterns.PatternStateMachine;

public class MarketEvent {
    private MessageType type;
    private JsonNode payload;
    private long timestamp;
    private PatternStateMachine triggeredMachine; // Field to carry the signal

    public enum MessageType {
        CANDLE_UPDATE,
        SENTIMENT_UPDATE,
        // Add other types as needed
    }

    // Getters and setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public PatternStateMachine getTriggeredMachine() {
        return triggeredMachine;
    }

    public void setTriggeredMachine(PatternStateMachine triggeredMachine) {
        this.triggeredMachine = triggeredMachine;
    }

    public void clear() {
        this.type = null;
        this.payload = null;
        this.timestamp = 0L;
        this.triggeredMachine = null; // Clear the signal
    }
}
