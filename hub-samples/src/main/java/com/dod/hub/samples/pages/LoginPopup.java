package com.dod.hub.samples.pages;

import com.dod.hub.facade.pagefactory.HubComponent;
import org.openqa.selenium.By;

/**
 * Sample Nested Component using HubComponent base.
 */
public class LoginPopup extends HubComponent {

    // No constructor needed!

    public String getText() {
        return root.getText();
    }

    // Example of finding within the component context using root manually
    // Ideally user would use @FindBy here too now that it is supported!
    public String getTitle() {
        return root.findElement(By.cssSelector(".title")).getText();
    }
}
