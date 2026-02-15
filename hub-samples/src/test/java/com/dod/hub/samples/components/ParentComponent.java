package com.dod.hub.samples.components;

import com.dod.hub.facade.pagefactory.HubComponent;
import org.openqa.selenium.support.FindBy;
import java.util.List;

public class ParentComponent extends HubComponent {

    @FindBy(css = ".single-child .child")
    private ChildComponent singleChild;

    @FindBy(css = ".child-list .child")
    private List<ChildComponent> childList;

    public ChildComponent getSingleChild() {
        return singleChild;
    }

    public List<ChildComponent> getChildList() {
        return childList;
    }

    public String getTitle() {
        // Assuming h3 is direct child or easily findable
        return getRoot().findElement(org.openqa.selenium.By.tagName("h3")).getText();
    }
}
