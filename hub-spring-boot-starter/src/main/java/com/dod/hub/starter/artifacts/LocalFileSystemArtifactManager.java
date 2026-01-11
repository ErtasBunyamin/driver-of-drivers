package com.dod.hub.starter.artifacts;

import com.dod.hub.core.config.HubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalFileSystemArtifactManager implements ArtifactManager {

    private static final Logger log = LoggerFactory.getLogger(LocalFileSystemArtifactManager.class);
    private final HubConfig config;

    public LocalFileSystemArtifactManager(HubConfig config) {
        this.config = config;
    }

    @Override
    public Path saveArtifact(String className, String methodName, String fileName, byte[] data) {
        if (data == null || data.length == 0) {
            log.warn("Attempted to save empty artifact: {}", fileName);
            return null;
        }

        try {
            Path baseDir = Paths.get(config.getArtifactPath());

            Path targetDir = baseDir.resolve(className).resolve(methodName);
            Files.createDirectories(targetDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss_SSS"));
            Path targetFile = targetDir.resolve(timestamp + "_" + fileName);

            Files.write(targetFile, data);
            log.info("Artifact saved: {}", targetFile.toAbsolutePath());
            return targetFile;

        } catch (IOException e) {
            log.error("Failed to save artifact: {}", fileName, e);
            return null;
        }
    }
}
