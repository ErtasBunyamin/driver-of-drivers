package com.dod.hub.starter.artifacts;

import java.nio.file.Path;

/**
 * Strategy interface for saving test artifacts.
 * Implementations can store artifacts on local disk, S3, or other storage
 * systems.
 */
public interface ArtifactManager {

    /**
     * Saves an artifact (e.g., screenshot, log file).
     *
     * @param className  The name of the test class.
     * @param methodName The name of the test method.
     * @param fileName   The desired filename (e.g., "screenshot.png").
     * @param data       The binary content of the artifact.
     * @return The path to the saved artifact (if local), or a reference URI.
     *         Returns null on failure.
     */
    Path saveArtifact(String className, String methodName, String fileName, byte[] data);
}
