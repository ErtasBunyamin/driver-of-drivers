package com.dod.hub.core.telemetry;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Immutable data class containing test execution results.
 */
public class HubTestResult {
    private final String testClass;
    private final String testMethod;
    private final HubTestEvent status;
    private final long durationMs;
    private final String errorMessage;
    private final List<Path> artifactPaths;
    private final Instant timestamp;

    public HubTestResult(String testClass, String testMethod, HubTestEvent status,
            long durationMs, String errorMessage, List<Path> artifactPaths) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.status = status;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.artifactPaths = artifactPaths;
        this.timestamp = Instant.now();
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public HubTestEvent getStatus() {
        return status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<Path> getArtifactPaths() {
        return artifactPaths;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
