package com.dod.hub.core.locator;

import java.util.Objects;

/**
 * Represents a standard locator (Strategy + Value).
 */
public class HubLocator {
    private final LocatorStrategy strategy;
    private final String value;

    public HubLocator(LocatorStrategy strategy, String value) {
        this.strategy = strategy;
        this.value = value;
    }

    public static HubLocator css(String value) {
        return new HubLocator(LocatorStrategy.CSS, value);
    }

    public static HubLocator xpath(String value) {
        return new HubLocator(LocatorStrategy.XPATH, value);
    }

    public static HubLocator id(String value) {
        return new HubLocator(LocatorStrategy.ID, value);
    }

    public static HubLocator name(String value) {
        return new HubLocator(LocatorStrategy.NAME, value);
    }

    public static HubLocator className(String value) {
        return new HubLocator(LocatorStrategy.CLASS_NAME, value);
    }

    public static HubLocator tagName(String value) {
        return new HubLocator(LocatorStrategy.TAG_NAME, value);
    }

    public LocatorStrategy getStrategy() {
        return strategy;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return strategy.name().toLowerCase() + "=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HubLocator that = (HubLocator) o;
        return strategy == that.strategy && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, value);
    }
}
