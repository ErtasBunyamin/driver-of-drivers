package com.dod.hub.facade.pagefactory;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.DefaultFieldDecorator;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Enhances the default field decorator to support the initialization of custom
 * Component objects
 * as well as standard WebElements.
 * 
 * The decoration logic first attempts standard Selenium decoration. If no
 * standard element is detected,
 * and the field type extends HubComponent, it instantiates the component and
 * injects a managed proxy.
 */
public class HubFieldDecorator extends DefaultFieldDecorator {

    private final DependencyInjector injector;

    public HubFieldDecorator(ElementLocatorFactory factory) {
        this(factory, null);
    }

    public HubFieldDecorator(ElementLocatorFactory factory, DependencyInjector injector) {
        super(factory);
        this.injector = injector;
    }

    @Override
    public Object decorate(ClassLoader loader, Field field) {
        // Try standard decoration first (WebElement, List<WebElement>)
        Object decorated = super.decorate(loader, field);
        if (decorated != null) {
            return decorated;
        }

        // Check for List<HubComponent>
        if (List.class.isAssignableFrom(field.getType())) {
            return decorateList(loader, field);
        }

        // Try to instantiate as a Component
        return decorateComponent(loader, field);
    }

    @SuppressWarnings("unchecked")
    private Object decorateList(ClassLoader loader, Field field) {
        // Check generic type
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType pt = (ParameterizedType) genericType;
        java.lang.reflect.Type listType = pt.getActualTypeArguments()[0];

        if (!(listType instanceof Class)) {
            return null;
        }

        Class<?> listClass = (Class<?>) listType;
        if (!HubComponent.class.isAssignableFrom(listClass)) {
            return null;
        }

        Class<? extends HubComponent> componentClass = (Class<? extends HubComponent>) listClass;

        ElementLocator locator = factory.createLocator(field);
        if (locator == null) {
            return null;
        }

        InvocationHandler handler = new HubComponentListHandler(locator, componentClass, injector);
        return Proxy.newProxyInstance(loader, new Class[] { List.class }, handler);
    }

    private Object decorateComponent(ClassLoader loader, Field field) {
        // Enforce HubComponent extension
        if (!HubComponent.class.isAssignableFrom(field.getType())) {
            return null;
        }

        ElementLocator locator = factory.createLocator(field);
        if (locator == null) {
            return null;
        }

        // Proxy the element
        WebElement proxy = proxyForLocator(loader, locator);

        try {
            // Instantiate using no-args constructor
            Object instance = field.getType().getDeclaredConstructor().newInstance();

            // Inject dependencies if an injector is provided
            if (injector != null) {
                injector.inject(instance);
            }

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
