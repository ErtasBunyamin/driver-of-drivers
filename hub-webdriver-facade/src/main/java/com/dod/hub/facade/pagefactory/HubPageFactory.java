package com.dod.hub.facade.pagefactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;

/**
 * Extended PageFactory that supports Nested Components.
 */
public class HubPageFactory {

    private static DependencyInjector globalInjector = instance -> {
    };

    public static void setGlobalDependencyInjector(DependencyInjector injector) {
        if (injector != null) {
            globalInjector = injector;
        }
    }

    public static void initElements(WebDriver driver, Object page) {
        PageFactory.initElements(
                new HubFieldDecorator(new DefaultElementLocatorFactory(driver), globalInjector),
                page);
    }
}
