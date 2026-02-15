package com.dod.hub.samples.pages;

import com.dod.hub.samples.components.ParentComponent;
import org.openqa.selenium.support.FindBy;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ComplexPage {

    @FindBy(css = "#parent-list .parent")
    private List<ParentComponent> parents;

    public List<ParentComponent> getParents() {
        return parents;
    }
}
