package com.dod.hub.core.provider;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import java.util.List;

/**
 * The SPI interface that all concrete automation providers (Selenium,
 * Playwright) must implement.
 */
public interface HubProvider {

    /**
     * Unique name of the provider (e.g. "selenium", "playwright").
     */
    String getName();

    /**
     * Starts a new browser session.
     */
    ProviderSession start(SessionCapabilities caps);

    /**
     * Stops the given session and closes the browser.
     */
    void stop(ProviderSession session);

    /**
     * Finds a single element. Throws exception if not found.
     */
    HubElementRef find(ProviderSession session, HubLocator locator);

    /**
     * Finds all matching elements. Returns empty list if none found.
     */
    List<HubElementRef> findAll(ProviderSession session, HubLocator locator);

    /**
     * Finds a single element relative to a parent element.
     */
    HubElementRef find(ProviderSession session, HubElementRef parent, HubLocator locator);

    /**
     * Finds all matching elements relative to a parent element.
     */
    List<HubElementRef> findAll(ProviderSession session, HubElementRef parent, HubLocator locator);

    // --- Actions ---

    void click(ProviderSession session, HubElementRef element);

    void type(ProviderSession session, HubElementRef element, String text);

    void clear(ProviderSession session, HubElementRef element);

    String getText(ProviderSession session, HubElementRef element);

    String getAttribute(ProviderSession session, HubElementRef element, String attributeName);

    boolean isDisplayed(ProviderSession session, HubElementRef element);

    boolean isEnabled(ProviderSession session, HubElementRef element);

    boolean isSelected(ProviderSession session, HubElementRef element);

    // --- Page Actions ---

    void navigate(ProviderSession session, String url);

    void back(ProviderSession session);

    void forward(ProviderSession session);

    void refresh(ProviderSession session);

    String getTitle(ProviderSession session);

    String getCurrentUrl(ProviderSession session);

    String getPageSource(ProviderSession session);

    /**
     * Captures a screenshot and returns the raw bytes.
     */
    byte[] takeScreenshot(ProviderSession session);

    /**
     * Sets timeouts for the session.
     */
    void setTimeouts(ProviderSession session, long implicitWaitMs, long pageLoadMs);
}
