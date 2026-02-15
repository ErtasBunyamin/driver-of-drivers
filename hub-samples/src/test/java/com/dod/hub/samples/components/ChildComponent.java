package com.dod.hub.samples.components;

import com.dod.hub.facade.pagefactory.HubComponent;
import org.openqa.selenium.support.FindBy;

public class ChildComponent extends HubComponent {

    @FindBy(className = "grandchild")
    private GrandChildComponent grandChild;

    public GrandChildComponent getGrandChild() {
        return grandChild;
    }

    public String getText() {
        return getRoot().getText();
    }
}
