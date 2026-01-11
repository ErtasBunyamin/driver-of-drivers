package com.dod.hub.starter;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Robust integration test for the Artifact system.
 * Verifies that screenshots are actually saved to the disk.
 */
@HubTest
@TestPropertySource(properties = {
        "hub.provider=SELENIUM",
        "hub.browser=CHROME",
        "hub.headless=true",
        "hub.artifacts.path=target/integ-artifacts",
        "hub.artifacts.policy=ALWAYS"
})
public class ArtifactIntegrationTest {

    private static final Path TEST_PATH = Paths.get("target/integ-artifacts");

    @HubDriver
    private HubWebDriver driver;

    @BeforeAll
    static void cleanup() throws IOException {
        FileSystemUtils.deleteRecursively(TEST_PATH);
    }

    @Test
    void shouldCaptureScreenshotOnSuccess() {
        driver.get("data:text/html,<html><body style='background:green'><h1>Success</h1></body></html>");
    }

    @AfterAll
    static void verifyFiles() throws IOException {
        if (Files.exists(TEST_PATH)) {
            try (Stream<Path> paths = Files.walk(TEST_PATH)) {
                long pngCount = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".png"))
                        .count();

                assertThat(pngCount)
                        .as("At least one screenshot should be saved in the target directory")
                        .isGreaterThanOrEqualTo(1);
            }
        } else {
            Assertions.fail("Artifact directory was not created: " + TEST_PATH.toAbsolutePath());
        }
    }
}
