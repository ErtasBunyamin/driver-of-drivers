package com.dod.hub.facade.pagefactory;

/**
 * Functional interface for dependency injection.
 * Allows HubFieldDecorator to request injection for created components
 * without depending on a specific DI framework (like Spring).
 */
@FunctionalInterface
public interface DependencyInjector {
    void inject(Object instance);
}
