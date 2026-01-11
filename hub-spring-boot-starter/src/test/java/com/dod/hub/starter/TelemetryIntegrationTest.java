package com.dod.hub.starter;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Telemetry system.
 * Verifies that hub-telemetry.json is created with expected events.
 */
@HubTest
@TestPropertySource(properties = {
        "hub.provider=SELENIUM",
        "hub.browser=CHROME",
        "hub.headless=true",
        "hub.artifacts.path=target/telemetry-test",
        "hub.telemetry.enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TelemetryIntegrationTest {

    private static final Path TELEMETRY_FILE = Paths.get("target/telemetry-test/hub-telemetry.json");

    @HubDriver
    private HubWebDriver driver;

    @BeforeAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(TELEMETRY_FILE);
    }

    @Test
    @Order(1)
    void testTelemetryEmission() {
        driver.get("data:text/html,<h1>Telemetry Test</h1>");
    }

    @AfterAll
    static void verifyTelemetryFile() throws IOException {
        assertThat(Files.exists(TELEMETRY_FILE))
                .as("Telemetry file should be created")
                .isTrue();

        String content = Files.readString(TELEMETRY_FILE);
        assertThat(content)
                .as("Telemetry should contain TEST_PASSED event")
                .contains("TEST_PASSED");
        assertThat(content)
                .as("Telemetry should contain test method name")
                .contains("testTelemetryEmission");
    }
}
