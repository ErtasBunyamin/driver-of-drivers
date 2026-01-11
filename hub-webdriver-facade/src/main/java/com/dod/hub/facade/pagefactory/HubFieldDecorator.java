package com.dod.hub.facade.pagefactory;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.DefaultFieldDecorator;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Custom decorator that supports initializing custom Component objects
 * in addition to standard WebElements.
 * 
 * Logic:
 * 1. If primitive/standard list -> delegate to super.
 * 2. If custom type -> try to find constructor(WebElement) and instantiate with
 * proxy.
 */
public class HubFieldDecorator extends DefaultFieldDecorator {

    public HubFieldDecorator(ElementLocatorFactory factory) {
        super(factory);
    }

    @Override
    public Object decorate(ClassLoader loader, Field field) {
        // Try standard decoration first (WebElement, List<WebElement>)
        Object decorated = super.decorate(loader, field);
        if (decorated != null) {
            return decorated;
        }

        // Check if it is a list of components - not supported in MVP
        if (List.class.isAssignableFrom(field.getType())) {
            return null;
        }

        // Try to instantiate as a Component
        return decorateComponent(loader, field);
    }

    private Object decorateComponent(ClassLoader loader, Field field) {
        // Enforce HubComponent extension
        if (!HubComponent.class.isAssignableFrom(field.getType())) {
            return null;
        }

        System.out.println("DEBUG: Decorating HubComponent field: " + field.getName());

        ElementLocator locator = factory.createLocator(field);
        if (locator == null) {
            return null;
        }

        // Proxy the element
        WebElement proxy = proxyForLocator(loader, locator);

        try {
            // Instantiate using no-args constructor
            Object instance = field.getType().getDeclaredConstructor().newInstance();

            // Initialize the component with the proxy root
            ((HubComponent) instance).init(proxy);

            return instance;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Component " + field.getType().getName() + " must have a public no-args constructor.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate component: " + field.getType().getName(), e);
        }
    }
}
