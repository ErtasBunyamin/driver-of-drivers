package com.dod.hub.starter.context;

import com.dod.hub.facade.HubWebDriver;

/**
 * Holds the current HubWebDriver for the active thread.
 * Used to support Dependency Injection of the driver into Page Objects.
 */
public class HubContext {

    private static final ThreadLocal<HubWebDriver> currentDriver = new ThreadLocal<>();

    public static void set(HubWebDriver driver) {
        currentDriver.set(driver);
    }

    public static HubWebDriver get() {
        return currentDriver.get();
    }

    public static void remove() {
        currentDriver.remove();
    }
}
