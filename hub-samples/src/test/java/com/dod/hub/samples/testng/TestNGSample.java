package com.dod.hub.samples.testng;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubFactory;
import com.dod.hub.facade.HubWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Demonstrates how to use Hub Automation Framework with TestNG.
 * Since we don't have a specific TestNG extension yet, we manage the driver
 * manually.
 */
public class TestNGSample {

    private HubWebDriver driver;

    @BeforeMethod
    public void setup() {
        HubConfig config = new HubConfig();
        config.setProvider(HubProviderType.SELENIUM); // or PLAYWRIGHT
        config.setHeadless(true);

        driver = HubFactory.create(config);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testGoogleTitle() {
        driver.get("https://google.com");
        String title = driver.getTitle();
        System.out.println("Title: " + title);
        Assert.assertTrue(title.contains("Google"), "Title should contain Google");
    }
}
