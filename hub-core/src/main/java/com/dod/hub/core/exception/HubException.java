package com.dod.hub.core.exception;

/**
 * Base exception for all Hub Framework related errors.
 * Unchecked exception to allow cleaner APIs, similar to Selenium's
 * WebDriverException.
 */
public class HubException extends RuntimeException {
    public HubException(String message) {
        super(message);
    }

    public HubException(String message, Throwable cause) {
        super(message, cause);
    }
}
