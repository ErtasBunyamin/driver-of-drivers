package com.dod.hub.core.command;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the outcome of a HubCommand execution.
 */
public class CommandResult {
    public enum Status {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    private Status status;
    private Object returnValue; // e.g., WebElement, String text, boolean
    private Throwable error;
    private String errorMessage;
    private Map<String, String> artifacts = new HashMap<>();

    public CommandResult() {}

    public static CommandResult success(Object returnValue) {
        CommandResult result = new CommandResult();
        result.status = Status.SUCCESS;
        result.returnValue = returnValue;
        return result;
    }

    public static CommandResult failure(Throwable error) {
        CommandResult result = new CommandResult();
        result.status = Status.FAILURE;
        result.error = error;
        result.errorMessage = error.getMessage();
        return result;
    }

    public void addArtifact(String key, String path) {
        this.artifacts.put(key, path);
    }

    // Getters and Setters
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Object getReturnValue() { return returnValue; }
    public void setReturnValue(Object returnValue) { this.returnValue = returnValue; }
    public Throwable getError() { return error; }
    public void setError(Throwable error) { this.error = error; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Map<String, String> getArtifacts() { return artifacts; }
    public void setArtifacts(Map<String, String> artifacts) { this.artifacts = artifacts; }
}
