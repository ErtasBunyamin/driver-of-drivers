package com.dod.hub.samples.cucumber.steps;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.samples.components.GreetingComponent;
import com.dod.hub.samples.pages.ComponentDemoPage;
import com.dod.hub.starter.context.HubContext;
import com.dod.hub.starter.pagefactory.HubSpringFactory;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ComponentInjectionSteps {

    @Autowired
    private HubSpringFactory hubSpringFactory;

    @Autowired
    private HubWebDriver driver; // Injected by HubAutoConfiguration (prototype)

    private ComponentDemoPage demoPage;
    private GreetingComponent greetingComponent;

    @Given("I navigate to the stability page for component test")
    public void navigateToStabilityPage() {
        // We can reuse the stability page for this demo, or just load any page.
        // The component uses @FindBy(id="greeting-section") which likely doesn't exist
        // on stability page,
        // but for unit testing the DI mechanism, the element doesn't need to be found
        // *unless* we interact with it via Selenium.
        // However, HubComponent initialization happens *before* interaction, so DI
        // should work regardless of element presence on page (until we call typical
        // methods).
        // BUT, HubFieldDecorator creates a PROXY for the WebElement.

        // Let's load the stability page to have a valid DOM.
        String path = "src/test/resources/stability/stability-page.html";
        String absPath = java.nio.file.Paths.get(path).toAbsolutePath().toUri().toString();
        driver.get(absPath);

        // Create the page using the factory which triggers component creation and
        // injection
        demoPage = hubSpringFactory.createPage(ComponentDemoPage.class);
    }

    @When("I access the greeting component on the demo page")
    public void accessGreetingComponent() {
        greetingComponent = demoPage.getGreetingComponent();
        assertNotNull(greetingComponent, "GreetingComponent should be instantiated by HubFieldDecorator");
    }

    @Then("the component should display the greeting {string}")
    public void verifyGreeting(String expectedGreeting) {
        String actualGreeting = greetingComponent.getGreeting();
        assertEquals(expectedGreeting, actualGreeting, "Service injection failed!");
    }

    @Then("the component should have an initialized root element")
    public void verifyRootElement() {
        assertNotNull(greetingComponent.getRoot(), "Root element should be initialized (proxy)");
    }

    @Then("the component list should not be null")
    public void verifyComponentList() {
        assertNotNull(demoPage.getGreetingList(), "List<GreetingComponent> should not be null");
        // We can't easily assert size > 0 because the stability page might not match
        // the selector,
        // but the list object itself MUST be initialized (proxy list), not null.
    }
}
