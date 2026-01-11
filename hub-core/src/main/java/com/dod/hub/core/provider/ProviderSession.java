package com.dod.hub.core.provider;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an active automation session.
 * Holds the specific driver object (WebDriver, Playwright Page, etc.)
 */
public class ProviderSession {
    private final String sessionId;
    private final String providerName;
    private final SessionCapabilities capabilities;
    private final Instant createdTime;

    // The raw driver object (e.g. org.openqa.selenium.WebDriver)
    private final Object rawDriver;

    public ProviderSession(String providerName, SessionCapabilities capabilities, Object rawDriver) {
        this.sessionId = UUID.randomUUID().toString();
        this.providerName = providerName;
        this.capabilities = capabilities;
        this.rawDriver = rawDriver;
        this.createdTime = Instant.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getProviderName() {
        return providerName;
    }

    public SessionCapabilities getCapabilities() {
        return capabilities;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public Object getRawDriver() {
        return rawDriver;
    }
}
