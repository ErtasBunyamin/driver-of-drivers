package com.dod.hub.core.exception;

/**
 * Thrown when an operation (like finding an element) times out.
 */
public class HubTimeoutException extends HubException {
    public HubTimeoutException(String message) {
        super(message);
    }

    public HubTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
