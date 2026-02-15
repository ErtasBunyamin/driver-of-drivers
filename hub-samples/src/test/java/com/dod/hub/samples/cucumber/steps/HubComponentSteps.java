package com.dod.hub.samples.cucumber.steps;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.samples.pages.ComplexPage;
import com.dod.hub.samples.components.ParentComponent;
import com.dod.hub.starter.pagefactory.HubSpringFactory;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HubComponentSteps {

    @Autowired
    private HubSpringFactory hubSpringFactory;

    @Autowired
    private HubWebDriver driver;

    private ComplexPage complexPage;
    private ParentComponent currentParent;

    @Given("I navigate to the complex components page")
    public void navigateToComplexPage() {
        String path = "src/test/resources/stability/complex-components.html";
        String absPath = java.nio.file.Paths.get(path).toAbsolutePath().toUri().toString();
        driver.get(absPath);
        complexPage = hubSpringFactory.createPage(ComplexPage.class);
    }

    @Then("I should find {int} parent components")
    public void verifyParentCount(int count) {
        assertNotNull(complexPage.getParents(), "Parent list should not be null");
        assertEquals(count, complexPage.getParents().size());
    }

    @When("I inspect parent {int}")
    public void inspectParent(int index) {
        // Cucumber is 1-based usually in scenarios, List is 0-based
        currentParent = complexPage.getParents().get(index - 1);
        assertNotNull(currentParent, "Parent at index " + index + " should not be null");
    }

    @Then("it should have title {string}")
    public void verifyTitle(String title) {
        assertEquals(title, currentParent.getTitle());
    }

    @Then("it should have a single child with grandchild content {string}")
    public void verifySingleChildGrandchild(String content) {
        assertNotNull(currentParent.getSingleChild(), "Single child should not be null");
        assertNotNull(currentParent.getSingleChild().getGrandChild(), "Grandchild should not be null");
        assertEquals(content, currentParent.getSingleChild().getGrandChild().getContent());
    }

    @Then("it should have a child list of size {int}")
    public void verifyChildListSize(int size) {
        assertNotNull(currentParent.getChildList(), "Child list should not be null");
        assertEquals(size, currentParent.getChildList().size());
    }
}
