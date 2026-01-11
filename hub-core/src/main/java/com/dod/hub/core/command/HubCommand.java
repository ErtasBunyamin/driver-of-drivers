package com.dod.hub.core.command;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single execution unit (action) in the Hub Framework.
 * This is the "Intermediate Representation" (IR) for MVP.
 */
public class HubCommand {
    public static final String TARGET_BROWSER = "browser";

    private String id;
    private CommandType type;
    private String sessionId;
    private String providerName;

    // Target identifier (e.g., locator string or element ID)
    private String target;

    // Parameters (e.g., text to type, timeout values)
    private Map<String, Object> params = new HashMap<>();

    // Timing
    private Instant timestampStart;
    private Instant timestampEnd;
    private long durationMs;

    // Result
    private CommandResult result;

    public HubCommand(CommandType type, String sessionId, String providerName) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.sessionId = sessionId;
        this.providerName = providerName;
        this.timestampStart = Instant.now();
    }

    public void complete(CommandResult result) {
        this.result = result;
        this.timestampEnd = Instant.now();
        this.durationMs = java.time.Duration.between(timestampStart, timestampEnd).toMillis();
    }

    public HubCommand addParam(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public CommandType getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Instant getTimestampStart() {
        return timestampStart;
    }

    public Instant getTimestampEnd() {
        return timestampEnd;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public CommandResult getResult() {
        return result;
    }
}
