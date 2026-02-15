package com.dod.hub.facade.pagefactory;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * InvocationHandler for List<HubComponent>.
 * Wraps a List<WebElement> found by the locator into a List<HubComponent>.
 */
public class HubComponentListHandler implements InvocationHandler {

    private final ElementLocator locator;
    private final Class<? extends HubComponent> componentType;
    private final DependencyInjector injector;

    public HubComponentListHandler(ElementLocator locator, Class<? extends HubComponent> componentType,
            DependencyInjector injector) {
        this.locator = locator;
        this.componentType = componentType;
        this.injector = injector;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<WebElement> elements = locator.findElements();
        List<HubComponent> components = new ArrayList<>();

        for (WebElement element : elements) {
            try {
                HubComponent component = componentType.getDeclaredConstructor().newInstance();
                if (injector != null) {
                    injector.inject(component);
                }
                component.init(element, injector);
                components.add(component);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate component: " + componentType.getName(), e);
            }
        }

        try {
            return method.invoke(components, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
