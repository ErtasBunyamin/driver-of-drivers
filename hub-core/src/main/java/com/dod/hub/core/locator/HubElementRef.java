package com.dod.hub.core.locator;

/**
 * A reference to a resolved element on a specific provider.
 * This wraps the actual provider object (WebElement, Locator, etc.)
 * but keeps the metadata about how it was found.
 */
public class HubElementRef {

    // The locator used to find this element (useful for re-finding if stale)
    private final HubLocator locator;

    // The opaque provider-specific object (e.g., org.openqa.selenium.WebElement)
    private final Object providerHandle;

    public HubElementRef(HubLocator locator, Object providerHandle) {
        this.locator = locator;
        this.providerHandle = providerHandle;
    }

    public HubLocator getLocator() {
        return locator;
    }

    public Object getProviderHandle() {
        return providerHandle;
    }

    @Override
    public String toString() {
        return "HubElementRef{locator=" + locator + "}";
    }
}
