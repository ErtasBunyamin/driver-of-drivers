package com.dod.hub.starter.telemetry;

import com.dod.hub.core.telemetry.HubTestEvent;
import com.dod.hub.core.telemetry.HubTestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default telemetry listener that writes events to a JSON file.
 */
public class JsonFileTelemetryListener implements TelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(JsonFileTelemetryListener.class);
    private final Path outputPath;
    private final ObjectMapper mapper;
    private final List<Map<String, Object>> events = new ArrayList<>();

    public JsonFileTelemetryListener(String basePath) {
        this.outputPath = Paths.get(basePath, "hub-telemetry.json");
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public synchronized void onEvent(HubTestEvent event, HubTestResult result) {
        Map<String, Object> entry = new java.util.LinkedHashMap<>();
        entry.put("event", event.name());
        entry.put("timestamp", java.time.Instant.now().toString());

        if (result != null) {
            entry.put("testClass", result.getTestClass());
            entry.put("testMethod", result.getTestMethod());
            entry.put("durationMs", result.getDurationMs());
            if (result.getErrorMessage() != null) {
                entry.put("errorMessage", result.getErrorMessage());
            }
            if (result.getArtifactPaths() != null && !result.getArtifactPaths().isEmpty()) {
                entry.put("artifacts", result.getArtifactPaths().stream()
                        .map(Path::toString)
                        .toList());
            }
        }

        events.add(entry);
        flush();
    }

    private void flush() {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, mapper.writeValueAsString(events),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write telemetry: {}", e.getMessage());
        }
    }
}
