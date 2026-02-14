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
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.logging.LogEntries;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.time.Duration;

public class StepDefinitions {

    // Ideally we would Autowire the driver, but the driver lifecycle is tricky in
    // Cucumber without HubExtension.
    // For this sample, we'll implement a simple manual lifecycle hook logic,
    // OR we can demonstrate how to use HubSpringFactory if we had the driver.

    // Simplest path for Sample: Manual Driver Management in Hooks, injecting it
    // into steps via field.

    private HubWebDriver driver;
    private ThreadLocal<Long> scenarioStartNs = new ThreadLocal<>();
    private ThreadLocal<Boolean> stabilityScenario = new ThreadLocal<>();
    private ThreadLocal<String> providerName = new ThreadLocal<>();
    private ThreadLocal<Set<String>> previousWindowHandles = new ThreadLocal<>();

    private static final long WAIT_TIMEOUT_MS = 2000;
    private static final long POLL_INTERVAL_MS = 50;

    private static final Map<String, ProviderStats> STABILITY_STATS = new ConcurrentHashMap<>();

    @Before
    public void setup(Scenario scenario) {
        Collection<String> tagNames = scenario.getSourceTagNames();
        HubConfig config = new HubConfig();
        if (tagNames.contains("@Playwright")) {
            config.setProvider(HubProviderType.PLAYWRIGHT);
        } else if (tagNames.contains("@Selenium")) {
            config.setProvider(HubProviderType.SELENIUM);
        } else if (tagNames.contains("@Hybrid")) {
            config.setProvider(HubProviderType.HYBRID);
            config.addOption("hybrid.cdp.port", 0);
        } else {
            throw new RuntimeException("Unknown Hub Provider Type");
        }
        boolean headless = !tagNames.contains("@Window");
        config.setHeadless(headless);
        driver = HubFactory.create(config);
        providerName.set(config.getProvider().name());
        stabilityScenario.set(tagNames.contains("@Stability"));
        scenarioStartNs.set(System.nanoTime());
        previousWindowHandles.set(new HashSet<>());

        // Optional: Set Context if we wanted to use HubSpringFactory
        HubContext.set(driver);
    }

    @After
    public void tearDown(Scenario scenario) {
        if (stabilityScenario.get()) {
            double durationMs = (System.nanoTime() - scenarioStartNs.get()) / 1_000_000.0;
            STABILITY_STATS
                    .computeIfAbsent(providerName.get(), k -> new ProviderStats())
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

    @Given("I open the local stability page")
    public void i_open_the_local_stability_page() {
        driver.get(getStabilityPageUrl());
    }

    @Then("the page title should contain {string}")
    public void the_page_title_should_contain(String expected) {
        String title = driver.getTitle();
        Assert.assertTrue(title.contains(expected));
    }

    @Then("I should see the header {string}")
    public void i_should_see_the_header(String expectedHeader) {
        // Simple assertion demo
        String header = driver.findElement(By.cssSelector("h1")).getText();
        Assert.assertTrue(header.contains(expectedHeader));
    }

    @Then("the delayed badge should appear")
    public void the_delayed_badge_should_appear() {
        waitUntil(() -> {
            try {
                WebElement el = driver.findElement(By.id("delayed"));
                return el.isDisplayed();
            } catch (Exception e) {
                return false;
            }
        }, WAIT_TIMEOUT_MS, "Delayed badge did not appear in time.");
    }

    @When("I fill the input with {string}")
    public void i_fill_the_input_with(String text) {
        WebElement input = driver.findElement(By.id("item-input"));
        input.clear();
        input.sendKeys(text);
    }

    @When("I click the add button")
    public void i_click_the_add_button() {
        driver.findElement(By.id("add-btn")).click();
    }

    @Then("the list should contain item {string}")
    public void the_list_should_contain_item(String expected) {
        List<WebElement> items = driver.findElements(By.cssSelector("#items li"));
        boolean found = false;
        for (WebElement el : items) {
            if (expected.equals(el.getText())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "Expected list item not found: " + expected);
    }

    @When("I add {int} items quickly")
    public void i_add_items_quickly(int count) {
        for (int i = 1; i <= count; i++) {
            WebElement input = driver.findElement(By.id("item-input"));
            input.clear();
            input.sendKeys("item-" + i);
            driver.findElement(By.id("add-btn")).click();
        }
    }

    @Then("the list should have {int} items")
    public void the_list_should_have_items(int expected) {
        int actual = driver.findElements(By.cssSelector("#items li")).size();
        Assert.assertEquals(actual, expected, "Unexpected list item count.");
    }

    @When("I trigger the async status update")
    public void i_trigger_the_async_status_update() {
        driver.findElement(By.id("async-btn")).click();
    }

    @Then("the status should be {string}")
    public void the_status_should_be(String expected) {
        waitUntil(() -> {
            try {
                String text = driver.findElement(By.id("status")).getText();
                return expected.equals(text);
            } catch (Exception e) {
                return false;
            }
        }, WAIT_TIMEOUT_MS, "Status did not reach expected value: " + expected);
    }

    @When("I toggle the details panel")
    public void i_toggle_the_details_panel() {
        driver.findElement(By.id("toggle-btn")).click();
    }

    @Then("the details panel should be {string}")
    public void the_details_panel_should_be(String state) {
        boolean shouldBeVisible = "visible".equalsIgnoreCase(state);
        waitUntil(() -> {
            try {
                boolean visible = driver.findElement(By.id("details")).isDisplayed();
                return visible == shouldBeVisible;
            } catch (Exception e) {
                return !shouldBeVisible;
            }
        }, WAIT_TIMEOUT_MS, "Details panel visibility did not match: " + state);
    }

    @Then("I capture a screenshot")
    public void i_capture_a_screenshot() {
        byte[] bytes = driver.getScreenshotAs(OutputType.BYTES);
        Assert.assertTrue(bytes != null && bytes.length > 0, "Screenshot capture failed or empty.");
    }

    @When("I open a new tab")
    public void i_open_a_new_tab() {
        Set<String> handles = driver.getWindowHandles();
        previousWindowHandles.get().clear();
        previousWindowHandles.get().addAll(handles);
        driver.switchTo().newWindow(WindowType.TAB);
        driver.get(getStabilityPageUrl());
    }

    @When("I switch to the newest window")
    public void i_switch_to_the_newest_window() {
        Set<String> current = driver.getWindowHandles();
        Set<String> previous = previousWindowHandles.get();
        String target = null;
        for (String handle : current) {
            if (!previous.contains(handle)) {
                target = handle;
                break;
            }
        }
        if (target == null && !current.isEmpty()) {
            target = current.iterator().next();
        }
        if (target != null) {
            driver.switchTo().window(target);
        }
    }

    @Then("I should have at least {int} windows")
    public void i_should_have_at_least_windows(int expected) {
        int count = driver.getWindowHandles().size();
        Assert.assertTrue(count >= expected, "Expected at least " + expected + " windows, got " + count);
    }

    @When("I set window position to {int} and {int}")
    public void i_set_window_position_to_and(int x, int y) {
        driver.manage().window().setPosition(new Point(x, y));
    }

    @Then("I can read window position")
    public void i_can_read_window_position() {
        Point pos = driver.manage().window().getPosition();
        Assert.assertNotNull(pos, "Window position is null.");
    }

    @When("I close the current window")
    public void i_close_the_current_window() {
        driver.close();
        Set<String> handles = driver.getWindowHandles();
        if (!handles.isEmpty()) {
            driver.switchTo().window(handles.iterator().next());
        }
    }

    @Then("I capture a screenshot file")
    public void i_capture_a_screenshot_file() {
        File file = driver.getScreenshotAs(OutputType.FILE);
        Assert.assertTrue(file != null && file.exists() && file.length() > 0, "Screenshot file missing or empty.");
    }

    @Then("I should be able to read browser logs")
    public void i_should_be_able_to_read_browser_logs() {
        try {
            Set<String> types = driver.manage().logs().getAvailableLogTypes();
            if (types.contains("browser")) {
                LogEntries entries = driver.manage().logs().get("browser");
                Assert.assertNotNull(entries);
            }
        } catch (Exception e) {
            Assert.fail("Log access failed: " + e.getMessage());
        }
    }

    @When("I set async script timeout to {int} ms")
    public void i_set_async_script_timeout_to_ms(int timeoutMs) {
        driver.manage().timeouts().setScriptTimeout(Duration.ofMillis(timeoutMs));
    }

    @Then("an async script with {int} ms delay should time out")
    public void an_async_script_with_delay_should_time_out(int delayMs) {
        boolean timedOut = false;
        try {
            runAsyncScript(delayMs);
        } catch (Exception e) {
            timedOut = true;
        }
        Assert.assertTrue(timedOut, "Expected async script to time out.");
    }

    @Then("an async script with {int} ms delay should succeed")
    public void an_async_script_with_delay_should_succeed(int delayMs) {
        Object result = runAsyncScript(delayMs);
        Assert.assertEquals(result, "ok", "Async script did not return expected result.");
    }

    private Object runAsyncScript(int delayMs) {
        String script = "var cb = arguments[arguments.length - 1];" +
                "setTimeout(function(){ cb('ok'); }, " + delayMs + ");";
        return driver.executeAsyncScript(script);
    }

    @AfterAll
    public static void reportStability() {
        if (STABILITY_STATS.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("=== Hub Provider Stability Summary ===");

        List<ProviderResult> results = new ArrayList<>();

        for (Map.Entry<String, ProviderStats> entry : STABILITY_STATS.entrySet()) {
            ProviderStats stats = entry.getValue().snapshot();
            if (stats.count == 0) {
                continue;
            }
            double mean = stats.meanMs();
            double std = stats.stdDevMs();
            double cv = mean > 0.0 ? std / mean : Double.POSITIVE_INFINITY;
            ProviderResult result = new ProviderResult();
            result.provider = entry.getKey();
            result.count = stats.count;
            result.failures = stats.failures;
            result.meanMs = mean;
            result.stdMs = std;
            result.cv = cv;
            result.minMs = stats.minMs;
            result.maxMs = stats.maxMs;
            result.p50Ms = stats.percentile(0.50);
            result.p90Ms = stats.percentile(0.90);
            result.p95Ms = stats.percentile(0.95);
            results.add(result);
        }

        if (results.isEmpty()) {
            return;
        }

        double fastestMean = results.stream()
                .mapToDouble(r -> r.meanMs)
                .min()
                .orElse(0.0);

        System.out.println("Scoring: score = meanMs * (1 + cv), speedIndex = meanMs / fastestMean");

        for (ProviderResult r : results) {
            double speedIndex = fastestMean > 0.0 ? r.meanMs / fastestMean : Double.POSITIVE_INFINITY;
            double slowdownPct = (speedIndex - 1.0) * 100.0;
            r.score = r.meanMs * (1.0 + r.cv);

            System.out.printf(
                    "%s -> runs=%d, failures=%d, mean=%.2fms, std=%.2fms, cv=%.3f, p50=%.2fms, p90=%.2fms, p95=%.2fms, min=%.2fms, max=%.2fms, speedIndex=%.3f, slowdown=+%.1f%%, score=%.2f%n",
                    r.provider,
                    r.count,
                    r.failures,
                    r.meanMs,
                    r.stdMs,
                    r.cv,
                    r.p50Ms,
                    r.p90Ms,
                    r.p95Ms,
                    r.minMs,
                    r.maxMs,
                    speedIndex,
                    slowdownPct,
                    r.score);
        }

        ProviderResult fastest = results.stream()
                .min(Comparator.comparingDouble(r -> r.meanMs))
                .orElse(null);
        ProviderResult mostStable = results.stream()
                .min(Comparator.comparingDouble(r -> r.cv))
                .orElse(null);
        ProviderResult bestOverall = results.stream()
                .min(Comparator.comparingInt((ProviderResult r) -> r.failures)
                        .thenComparingDouble(r -> r.score))
                .orElse(null);

        if (fastest != null) {
            System.out.printf("Fastest provider: %s (mean=%.2fms)%n", fastest.provider, fastest.meanMs);
        }
        if (mostStable != null) {
            System.out.printf("Most stable provider: %s (cv=%.3f, failures=%d)%n",
                    mostStable.provider, mostStable.cv, mostStable.failures);
        }
        if (bestOverall != null) {
            System.out.printf("Best overall (speed+stability): %s (score=%.2f, failures=%d)%n",
                    bestOverall.provider, bestOverall.score, bestOverall.failures);
        }
    }

    private String getStabilityPageUrl() {
        URL url = StepDefinitions.class.getResource("/stability/stability-page.html");
        if (url == null) {
            throw new RuntimeException("Local stability page not found on classpath.");
        }
        return url.toString();
    }

    private void waitUntil(BooleanSupplier condition, long timeoutMs, String errorMessage) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.fail(errorMessage);
    }

    private static class ProviderResult {
        private String provider;
        private int count;
        private int failures;
        private double meanMs;
        private double stdMs;
        private double cv;
        private double minMs;
        private double maxMs;
        private double p50Ms;
        private double p90Ms;
        private double p95Ms;
        private double score;
    }

    private static class ProviderStats {
        private int count;
        private int failures;
        private double sumMs;
        private double sumSqMs;
        private double minMs = Double.MAX_VALUE;
        private double maxMs = Double.MIN_VALUE;
        private final List<Double> samples = new ArrayList<>();

        synchronized void record(double durationMs, boolean failed) {
            count++;
            if (failed) {
                failures++;
            }
            sumMs += durationMs;
            sumSqMs += durationMs * durationMs;
            minMs = Math.min(minMs, durationMs);
            maxMs = Math.max(maxMs, durationMs);
            samples.add(durationMs);
        }

        synchronized ProviderStats snapshot() {
            ProviderStats copy = new ProviderStats();
            copy.count = this.count;
            copy.failures = this.failures;
            copy.sumMs = this.sumMs;
            copy.sumSqMs = this.sumSqMs;
            copy.minMs = this.minMs == Double.MAX_VALUE ? 0.0 : this.minMs;
            copy.maxMs = this.maxMs == Double.MIN_VALUE ? 0.0 : this.maxMs;
            copy.samples.addAll(this.samples);
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

        double percentile(double p) {
            if (samples.isEmpty()) {
                return 0.0;
            }
            List<Double> sorted = new ArrayList<>(samples);
            Collections.sort(sorted);
            int index = (int) Math.ceil(p * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(index);
        }
    }

    // ==================== Capabilities ====================

    private Capabilities lastCapabilities;

    @Then("the driver should expose capabilities")
    public void the_driver_should_expose_capabilities() {
        Assert.assertTrue(driver instanceof HasCapabilities, "Driver should implement HasCapabilities");
        lastCapabilities = ((HasCapabilities) driver).getCapabilities();
        Assert.assertNotNull(lastCapabilities, "Capabilities should not be null");
    }

    @Then("the capabilities should contain {string}")
    public void the_capabilities_should_contain(String key) {
        Assert.assertNotNull(lastCapabilities, "Call 'the driver should expose capabilities' first");
        Object value = lastCapabilities.getCapability(key);
        Assert.assertNotNull(value, "Capability '" + key + "' should be present but was null");
    }

    @Then("the capabilities should not contain {string}")
    public void the_capabilities_should_not_contain(String key) {
        Assert.assertNotNull(lastCapabilities, "Call 'the driver should expose capabilities' first");
        Object value = lastCapabilities.getCapability(key);
        Assert.assertNull(value, "Capability '" + key + "' should NOT be present but was: " + value);
    }
}
