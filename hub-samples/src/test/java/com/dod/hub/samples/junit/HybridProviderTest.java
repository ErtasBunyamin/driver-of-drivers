package com.dod.hub.samples.junit;

import com.dod.hub.core.config.HubBrowserType;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.samples.HubSamplesApplication;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@HubTest
@SpringBootTest(classes = HubSamplesApplication.class)
public class HybridProviderTest {

    @HubDriver(provider = HubProviderType.HYBRID, browser = HubBrowserType.CHROME)
    private HubWebDriver driver;

    @Test
    void testHybridSessionNavigatesSuccessfully() {
        driver.get("https://www.selenium.dev/");

        String title = driver.getTitle();
        assertNotNull(title);
        assertTrue(title.toLowerCase().contains("selenium"), "Expected Selenium in title, got: " + title);

        String url = driver.getCurrentUrl();
        assertTrue(url.contains("selenium.dev"), "Expected selenium.dev in URL");
    }

    @Test
    void testHybridScreenshotUsingPlaywright() {
        driver.get("https://playwright.dev/");

        byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        assertNotNull(screenshot);
        assertTrue(screenshot.length > 1000, "Screenshot should have reasonable size");
    }
}
