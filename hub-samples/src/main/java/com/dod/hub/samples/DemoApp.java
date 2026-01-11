package com.dod.hub.samples;

import com.dod.hub.core.config.HubProviderType; // Added
import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.HubFactory;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.HubDriverFactory; // Added
import com.dod.hub.samples.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DemoApp {
    public static void main(String[] args) {
        System.out.println("=== Hub Framework Demo ===");

        // 1. Run with Selenium
        runScenario(HubProviderType.SELENIUM);

        System.out.println("\n----------------------------------\n");

        // 2. Run with Playwright
        runScenario(HubProviderType.PLAYWRIGHT);

        System.out.println("\n----------------------------------\n");

        // 3. Run with Factory Override (Manual Verification)
        runFactoryOverride();

        System.out.println("\n----------------------------------\n");

        // 4. Run PageFactory Verification (Playwright)
        runPageFactoryScenario();

        System.out.println("\n=== Demo Complete ===");
    }

    private static void runScenario(HubProviderType providerType) {
        System.out.println(">>> Starting Provider: " + providerType);

        HubConfig config = new HubConfig();
        config.setProvider(providerType);
        config.setHeadless(true); // Headless for CI/Execution speed
        config.setImplicitWaitMs(2000);

        HubWebDriver driver = null;
        try {
            driver = HubFactory.create(config);

            // Navigate
            String url = "https://example.com";
            System.out.println("Navigating to: " + url);
            driver.get(url);

            // Verify Title
            System.out.println("Title: " + driver.getTitle());

            // Find Element
            WebElement h1 = driver.findElement(By.cssSelector("h1"));
            System.out.println("H1 Text: " + h1.getText());

            if (!"Example Domain".equals(h1.getText())) {
                System.err.println("TEST FAILED: valid text not found");
            }

            // Interact
            WebElement link = driver.findElement(By.linkText("Learn more"));
            System.out.println("Found link: " + link.getText());
            link.click();

            System.out.println("New URL: " + driver.getCurrentUrl());

            // Screenshot
            byte[] screenshot = driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            System.out.println("Captured screenshot: " + screenshot.length + " bytes");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Session closed.");
            }
        }
    }

    private static void runFactoryOverride() {
        System.out.println(">>> Starting Factory Override Verification");
        HubConfig defaults = new HubConfig();
        defaults.setProvider(HubProviderType.SELENIUM);
        defaults.setHeadless(true);

        HubDriverFactory factory = new HubDriverFactory(defaults);

        // Override to Playwright via Config
        HubConfig override = new HubConfig();
        override.setProvider(HubProviderType.PLAYWRIGHT);
        override.setHeadless(true);
        // Copy others... simplified for demo

        try {
            HubWebDriver driver = factory.create(override);
            System.out.println("Override Driver Provider: " + driver.getSession().getProviderName());
            if (!"playwright".equalsIgnoreCase(driver.getSession().getProviderName())) {
                System.err.println("OVERRIDE FAILED: Expected playwright");
            } else {
                System.out.println("OVERRIDE SUCCESS");
            }
            driver.quit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runPageFactoryScenario() {
        System.out.println(">>> Starting PageFactory Verification (Provider: PLAYWRIGHT)");
        HubConfig config = new HubConfig();
        config.setProvider(HubProviderType.PLAYWRIGHT);
        config.setHeadless(true);

        try {
            HubWebDriver driver = HubFactory.create(config);
            driver.get("https://example.com");

            // Init Page Object
            LoginPage loginPage = new LoginPage(driver);

            // Interact using Page Object
            System.out.println("PF Header: " + loginPage.getHeaderText());
            loginPage.clickMoreLink();
            System.out.println("PF New URL: " + driver.getCurrentUrl());

            if (driver.getCurrentUrl().contains("iana.org")) {
                System.out.println("PAGE FACTORY SUCCESS");
            } else {
                System.err.println("PAGE FACTORY FAILED: URL did not change");
            }

            // Verify Component
            if (loginPage.getPopup() != null && loginPage.getPopup().getText().contains("Example Domain")) {
                System.out.println("COMPONENT SUCCESS: Popup text verified");
            } else {
                System.err.println("COMPONENT FAILED: Popup is null or text mismatch");
            }

            driver.quit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
