package com.dod.hub.samples.cucumber;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubFactory;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.context.HubContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.testng.Assert;

public class StepDefinitions {

    // Ideally we would Autowire the driver, but the driver lifecycle is tricky in
    // Cucumber without HubExtension.
    // For this sample, we'll implement a simple manual lifecycle hook logic,
    // OR we can demonstrate how to use HubSpringFactory if we had the driver.

    // Simplest path for Sample: Manual Driver Management in Hooks, injecting it
    // into steps via field.

    private HubWebDriver driver;

    @Before
    public void setup() {
        HubConfig config = new HubConfig();
        config.setProvider(HubProviderType.PLAYWRIGHT);
        config.setHeadless(true);
        driver = HubFactory.create(config);

        // Optional: Set Context if we wanted to use HubSpringFactory
        HubContext.set(driver);
    }

    @After
    public void tearDown() {
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
}
