package com.dod.hub.samples.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Bridges Cucumber and Spring Boot.
 * Warning: Cucumber doesn't support @HubTest directly on StepDefinitions
 * because
 * step definitions are instantiated differently.
 * Instead, we use @CucumberContextConfiguration to load the Spring Context,
 * and @HubTest (or @HubExtension) allows bean creation.
 * 
 * However, `HubExtension` hooks into JUnit 5 lifecycle. Cucumber runs with its
 * own engine.
 * So `HubExtension.beforeEach` might NOT trigger automatically for Cucumber
 * Scenarios!
 * 
 * DESIGN DECISION:
 * For Cucumber + Spring, we rely on Spring's Dependency Injection.
 * We need `HubWebDriver` to be available.
 * `HubAutoConfiguration` exposes `HubWebDriver` bean (proxy to thread-local).
 * BUT `HubExtension` handles the *Lifecycle* (Start/Stop) and populating
 * ThreadLocal.
 * 
 * Since we don't have a `HubCucumberHooks` yet, we must implement lifecycle
 * hooks manually here for now.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = com.dod.hub.samples.TestConfig.class)
public class CucumberConfiguration {
    // This class is just for configuration
}
