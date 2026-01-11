package com.dod.hub.facade.pagefactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;

/**
 * Extended PageFactory that supports Nested Components.
 */
public class HubPageFactory {

    public static void initElements(WebDriver driver, Object page) {
        PageFactory.initElements(
                new HubFieldDecorator(new DefaultElementLocatorFactory(driver)),
                page);
    }
}
