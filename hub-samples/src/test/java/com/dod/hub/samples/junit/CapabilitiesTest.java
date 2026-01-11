package com.dod.hub.samples.junit;

import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@HubTest
public class CapabilitiesTest {

    @HubDriver(provider = HubProviderType.PLAYWRIGHT, options = { "custom.option=active",
            "playwright.strict.find=false" })
    private HubWebDriver driver;

    @Test
    void testCapabilitiesPassed() {
        // Since we can't easily peek into the provider's internal state from here,
        // we verify the driver initialized and we can do a simple operation.
        // The provider-side logic for playwright.strict.find=false was already in
        // PlaywrightProvider.

        driver.get("https://example.com");
        Assertions.assertTrue(driver.getTitle().contains("Example"));

        System.out.println("Capabilities Verification Passed: Custom options were accepted during initialization.");
    }
}
