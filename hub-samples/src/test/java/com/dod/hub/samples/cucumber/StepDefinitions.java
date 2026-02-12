package com.dod.hub.samples.cucumber;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubFactory;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.context.HubContext;
import io.cucumber.java.AfterAll;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.testng.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StepDefinitions {

    // Ideally we would Autowire the driver, but the driver lifecycle is tricky in
    // Cucumber without HubExtension.
    // For this sample, we'll implement a simple manual lifecycle hook logic,
    // OR we can demonstrate how to use HubSpringFactory if we had the driver.

    // Simplest path for Sample: Manual Driver Management in Hooks, injecting it
    // into steps via field.

    private HubWebDriver driver;
    private long scenarioStartNs;
    private boolean stabilityScenario;
    private String providerName;

    private static final Map<String, ProviderStats> STABILITY_STATS = new ConcurrentHashMap<>();

    @Before
    public void setup(Scenario scenario) {
        Collection<String> tagNames = scenario.getSourceTagNames();
        HubConfig config = new HubConfig();
        if(tagNames.contains("@Playwright")) {
            config.setProvider(HubProviderType.PLAYWRIGHT);
        } else if(tagNames.contains("@Selenium")) {
            config.setProvider(HubProviderType.SELENIUM);
        } else if(tagNames.contains("@Hybrid")) {
            config.setProvider(HubProviderType.HYBRID);
            config.addOption("hybrid.cdp.port", 0);
        } else {
            throw new RuntimeException("Unknown Hub Provider Type");
        }
        config.setHeadless(false);
        driver = HubFactory.create(config);
        providerName = config.getProvider().name();
        stabilityScenario = tagNames.contains("@Stability");
        scenarioStartNs = System.nanoTime();

        // Optional: Set Context if we wanted to use HubSpringFactory
        HubContext.set(driver);
    }

    @After
    public void tearDown(Scenario scenario) {
        if (stabilityScenario) {
            double durationMs = (System.nanoTime() - scenarioStartNs) / 1_000_000.0;
            STABILITY_STATS
                    .computeIfAbsent(providerName, k -> new ProviderStats())
                    .record(durationMs, scenario.isFailed());
        }
        if (driver != null) {
            driver.quit();
        }
        HubContext.remove();
    }

    @Given("I navigate to {string}")
    public void i_navigate_to(String url) {
        driver.get(url);
    }

    @Then("the page title should contain {string}")
    public void the_page_title_should_contain(String expected) {
        String title = driver.getTitle();
        Assert.assertTrue(title.contains(expected));
    }

    @Then("I should see the header {string}")
    public void i_should_see_the_header(String expectedHeader) {
        // Simple assertion demo
        String header = driver.findElement(org.openqa.selenium.By.cssSelector("h1")).getText();
        Assert.assertTrue(header.contains(expectedHeader));
    }

    @AfterAll
    public static void reportStability() {
        if (STABILITY_STATS.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("=== Hub Provider Stability Summary ===");

        String bestProvider = null;
        double bestCv = Double.MAX_VALUE;
        int bestFailures = Integer.MAX_VALUE;

        for (Map.Entry<String, ProviderStats> entry : STABILITY_STATS.entrySet()) {
            ProviderStats stats = entry.getValue().snapshot();
            if (stats.count == 0) {
                continue;
            }
            double mean = stats.meanMs();
            double std = stats.stdDevMs();
            double cv = mean > 0.0 ? std / mean : Double.POSITIVE_INFINITY;

            System.out.printf(
                    "%s -> runs=%d, failures=%d, mean=%.2fms, std=%.2fms, cv=%.3f, min=%.2fms, max=%.2fms%n",
                    entry.getKey(),
                    stats.count,
                    stats.failures,
                    mean,
                    std,
                    cv,
                    stats.minMs,
                    stats.maxMs);

            if (stats.failures < bestFailures || (stats.failures == bestFailures && cv < bestCv)) {
                bestFailures = stats.failures;
                bestCv = cv;
                bestProvider = entry.getKey();
            }
        }

        if (bestProvider != null) {
            System.out.printf("Most stable provider: %s (cv=%.3f, failures=%d)%n", bestProvider, bestCv, bestFailures);
        }
    }

    private static class ProviderStats {
        private int count;
        private int failures;
        private double sumMs;
        private double sumSqMs;
        private double minMs = Double.MAX_VALUE;
        private double maxMs = Double.MIN_VALUE;

        synchronized void record(double durationMs, boolean failed) {
            count++;
            if (failed) {
                failures++;
            }
            sumMs += durationMs;
            sumSqMs += durationMs * durationMs;
            minMs = Math.min(minMs, durationMs);
            maxMs = Math.max(maxMs, durationMs);
        }

        synchronized ProviderStats snapshot() {
            ProviderStats copy = new ProviderStats();
            copy.count = this.count;
            copy.failures = this.failures;
            copy.sumMs = this.sumMs;
            copy.sumSqMs = this.sumSqMs;
            copy.minMs = this.minMs == Double.MAX_VALUE ? 0.0 : this.minMs;
            copy.maxMs = this.maxMs == Double.MIN_VALUE ? 0.0 : this.maxMs;
            return copy;
        }

        double meanMs() {
            return count == 0 ? 0.0 : sumMs / count;
        }

        double stdDevMs() {
            if (count == 0) {
                return 0.0;
            }
            double mean = meanMs();
            double variance = (sumSqMs / count) - (mean * mean);
            return Math.sqrt(Math.max(0.0, variance));
        }
    }
}
