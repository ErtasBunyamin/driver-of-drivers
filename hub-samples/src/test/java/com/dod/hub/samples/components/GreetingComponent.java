package com.dod.hub.samples.components;

import com.dod.hub.facade.pagefactory.HubComponent;
import com.dod.hub.samples.service.GreetingService;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Adding @Component is not strictly necessary for HubComponent,
// as HubFieldDecorator instantiates it.
// However, the test will verify that Autowiring happens regardless.
public class GreetingComponent extends HubComponent {

    @Autowired
    private GreetingService greetingService;

    @FindBy(tagName = "button")
    private WebElement submitButton;

    public String getGreeting() {
        if (greetingService == null) {
            return "GreetingService is null!";
        }
        return greetingService.getGreeting();
    }

    public WebElement getSubmitButton() {
        return submitButton;
    }
}
