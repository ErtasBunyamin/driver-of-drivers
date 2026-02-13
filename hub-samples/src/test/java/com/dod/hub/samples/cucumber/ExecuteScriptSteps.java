package com.dod.hub.samples.cucumber;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.context.HubContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class ExecuteScriptSteps {

    private Object scriptResult;
    private WebElement foundElement;

    // Helper to get driver from context (set by StepDefinitions @Before)
    private HubWebDriver getDriver() {
        return HubContext.get();
    }

    @When("I execute script {string} with arguments {int} and {int}")
    public void i_execute_script_with_arguments(String script, int arg1, int arg2) {
        scriptResult = getDriver().executeScript(script, arg1, arg2);
    }

    @Then("the script result should be {int}")
    public void the_script_result_should_be(int expected) {
        // Result might be Long or Integer depending on provider/JSON parsing
        Number result = (Number) scriptResult;
        Assert.assertEquals(result.intValue(), expected);
    }

    @When("I find the element by ID {string}")
    public void i_find_the_element_by_id(String id) {
        foundElement = getDriver().findElement(By.id(id));
        Assert.assertNotNull(foundElement, "Element with ID " + id + " not found");
    }

    @When("I execute script {string} on the found element")
    public void i_execute_script_on_the_found_element(String script) {
        // Pass the HubWebElement as argument
        getDriver().executeScript(script, foundElement);
    }

    @Then("the element value should be {string}")
    public void the_element_value_should_be(String expected) {
        // Use executeScript to retrieve value, verifying bi-directional HubWebElement
        // support
        Object result = getDriver().executeScript("return arguments[0].value;", foundElement);
        Assert.assertEquals(result, expected);
    }
}
