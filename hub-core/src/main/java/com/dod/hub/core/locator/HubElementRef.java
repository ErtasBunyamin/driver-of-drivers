package com.dod.hub.core.locator;

/**
 * A reference to a resolved element on a specific provider.
 * This class encapsulates the provider-specific element implementation (e.g.,
 * Selenium WebElement or Playwright Locator)
 * along with the metadata used for its retrieval.
 */
public class HubElementRef {

    private final HubLocator locator;
    private final Object providerHandle;

    public HubElementRef(HubLocator locator, Object providerHandle) {
        this.locator = locator;
        this.providerHandle = providerHandle;
    }

    public HubLocator locator() {
        return locator;
    }

    public Object handle() {
        return providerHandle;
    }

    // Legacy getters removed/renamed to avoid JSON recursion
    public HubLocator getLocator() {
        return locator;
    }

    @Override
    public String toString() {
        return "HubElementRef{locator=" + locator + "}";
    }
}
