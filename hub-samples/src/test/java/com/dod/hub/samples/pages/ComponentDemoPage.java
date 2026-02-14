package com.dod.hub.samples.pages;

import com.dod.hub.samples.components.GreetingComponent;
import org.openqa.selenium.support.FindBy;
import org.springframework.stereotype.Component;

@Component
public class ComponentDemoPage {

    // HubPageFactory will detect this as a HubComponent
    // and attempt to inject dependencies after instantiation.
    @FindBy(id = "greeting-section")
    private GreetingComponent greetingComponent;

    public GreetingComponent getGreetingComponent() {
        return greetingComponent;
    }

    @FindBy(className = "greeting")
    private java.util.List<GreetingComponent> greetingList;

    public java.util.List<GreetingComponent> getGreetingList() {
        return greetingList;
    }
}
