package com.dod.hub.samples.components;

import com.dod.hub.facade.pagefactory.HubComponent;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.WebElement;

public class GrandChildComponent extends HubComponent {

    // We can find by class or generic tag inside the root
    // Root is the .grandchild div

    public String getContent() {
        return getRoot().getText();
    }
}
