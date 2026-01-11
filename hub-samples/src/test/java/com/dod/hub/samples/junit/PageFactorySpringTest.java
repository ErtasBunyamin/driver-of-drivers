package com.dod.hub.samples.junit;

import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.samples.pages.LoginPage;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import com.dod.hub.facade.HubWebDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@HubTest
@Execution(ExecutionMode.CONCURRENT)
public class PageFactorySpringTest {

    @HubDriver(provider = HubProviderType.PLAYWRIGHT)
    private HubWebDriver playwrightDriver;

    @HubDriver(provider = HubProviderType.SELENIUM)
    private HubWebDriver seleniumDriver;

    @Test
    void testPlaywrightPageFactory() {
        verifyScenario(playwrightDriver, "Playwright");
    }

    @Test
    void testSeleniumPageFactory() {
        verifyScenario(seleniumDriver, "Selenium");
    }

    private void verifyScenario(HubWebDriver driver, String providerName) {
        System.out.println(
                "[" + providerName + "] Starting PageFactory Test thread: " + Thread.currentThread().getName());

        driver.get("https://example.com");

        LoginPage loginPage = new LoginPage(driver);

        // check header
        Assertions.assertTrue(loginPage.getHeaderText().contains("Example Domain"),
                providerName + ": Header verification failed");

        // check component
        Assertions.assertNotNull(loginPage.getPopup(), providerName + ": Popup component should not be null");
        Assertions.assertTrue(loginPage.getPopup().getText().contains("Example Domain"),
                providerName + ": Popup text verification failed");

        loginPage.clickMoreLink();

        System.out.println("[" + providerName + "] Finished PageFactory Test");
    }
}
