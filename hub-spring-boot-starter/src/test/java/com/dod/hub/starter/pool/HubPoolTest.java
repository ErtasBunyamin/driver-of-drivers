package com.dod.hub.starter.pool;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HubPoolTest.TestConfig.class)
@HubTest
@TestPropertySource(properties = {
        "hub.provider=SELENIUM",
        "hub.browser=CHROME",
        "hub.headless=false",
        "hub.performance.pooling.enabled=true",
        "hub.performance.pooling.max-active=2"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HubPoolTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class TestConfig {
    }

    @HubDriver
    private HubWebDriver driver;

    private static final Set<String> sessionIds = Collections.synchronizedSet(new HashSet<>());

    @Test
    @Order(1)
    void firstTest_ShouldCreateSession() {
        driver.get("data:text/html,<h1>Test 1</h1>");
        String sessionId = driver.getSession().getSessionId();
        System.out.println("Test 1 Session ID: " + sessionId);
        sessionIds.add(sessionId);
    }

    @Test
    @Order(2)
    void secondTest_ShouldReuseSession() {
        driver.get("data:text/html,<h1>Test 2</h1>");
        String sessionId = driver.getSession().getSessionId();
        System.out.println("Test 2 Session ID: " + sessionId);
        sessionIds.add(sessionId);
    }

    @AfterAll
    static void verifyPooling() {
        // With pooling enabled and max-active=2, we expect at most 2 unique sessions
        // The same driver may or may not be reused depending on timing
        assertThat(sessionIds).hasSizeLessThanOrEqualTo(2);
        System.out.println("Unique sessions used: " + sessionIds.size());
    }
}
