package com.dod.hub.facade.pagefactory;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;

/**
 * Base class for Nested Components.
 * Allows users to define components without constructor boilerplate.
 * The framework will automatically call init(root) after instantiation.
 */
public abstract class HubComponent {

    protected WebElement root;

    /**
     * Called by HubFieldDecorator to inject the root element
     * and initialize internal @FindBy fields relative to this root.
     */
    public void init(WebElement root) {
        // Initialize fields inside this component, scoped to the root element
        PageFactory.initElements(new DefaultElementLocatorFactory(root), this);
        this.root = root;
    }

    public WebElement getRoot() {
        return root;
    }
}
