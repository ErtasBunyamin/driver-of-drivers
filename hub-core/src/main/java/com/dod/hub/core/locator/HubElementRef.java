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
