package com.linkwave.app.domain.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * WebSocket message envelope for all client-server communication.
 * Defines the application protocol for realtime messaging.
 * 
 * Format:
 * {
 *   "event": "ping|pong|chat.send",
 *   "to": "+1234567890",  // optional, used for chat.send
 *   "payload": {}         // event-specific payload
 * }
 */
public class WsMessageEnvelope {
    
    @NotBlank(message = "Event field is required")
    @JsonProperty("event")
    private String event;
    
    @JsonProperty("to")
    private String to;
    
    @JsonProperty("payload")
    private JsonNode payload;
    
    public WsMessageEnvelope() {
    }
    
    public WsMessageEnvelope(String event, String to, JsonNode payload) {
        this.event = event;
        this.to = to;
        this.payload = payload;
    }
    
    public String getEvent() {
        return event;
    }
    
    public void setEvent(String event) {
        this.event = event;
    }
    
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public JsonNode getPayload() {
        return payload;
    }
    
    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
    
    /**
     * Supported event types for Phase C1.
     */
    public enum EventType {
        PING("ping"),
        PONG("pong"),
        CHAT_SEND("chat.send");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static boolean isValid(String event) {
            if (event == null) {
                return false;
            }
            for (EventType type : values()) {
                if (type.value.equals(event)) {
                    return true;
                }
            }
            return false;
        }
        
        public static EventType fromString(String event) {
            for (EventType type : values()) {
                if (type.value.equals(event)) {
                    return type;
                }
            }
            return null;
        }
    }
}
