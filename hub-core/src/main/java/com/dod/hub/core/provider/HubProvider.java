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

    /**
     * Executes a click action on the specified element.
     *
     * @param session The active provider session.
     * @param element The reference to the element to be clicked.
     */
    void click(ProviderSession session, HubElementRef element);

    /**
     * Types the specified text into the element.
     *
     * @param session The active provider session.
     * @param element The reference to the target element.
     * @param text    The string content to enter.
     */
    void type(ProviderSession session, HubElementRef element, String text);

    /**
     * Clears the content of the specified element (e.g., an input field).
     *
     * @param session The active provider session.
     * @param element The reference to the target element.
     */
    void clear(ProviderSession session, HubElementRef element);

    /**
     * Retrieves the visible text of the specified element.
     *
     * @param session The active provider session.
     * @param element The reference to the element.
     * @return The visible text content.
     */
    String getText(ProviderSession session, HubElementRef element);

    /**
     * Retrieves the value of a specific attribute from the element.
     *
     * @param session       The active provider session.
     * @param element       The reference to the element.
     * @param attributeName The name of the attribute to retrieve.
     * @return The attribute value, or null if not present.
     */
    String getAttribute(ProviderSession session, HubElementRef element, String attributeName);

    /**
     * Checks if the element is currently displayed in the UI.
     *
     * @param session The active provider session.
     * @param element The reference to the element.
     * @return true if visible, false otherwise.
     */
    boolean isDisplayed(ProviderSession session, HubElementRef element);

    /**
     * Checks if the element is enabled for interaction.
     *
     * @param session The active provider session.
     * @param element The reference to the element.
     * @return true if enabled, false otherwise.
     */
    boolean isEnabled(ProviderSession session, HubElementRef element);

    /**
     * Checks if the element (radio, checkbox, or option) is selected.
     *
     * @param session The active provider session.
     * @param element The reference to the element.
     * @return true if selected, false otherwise.
     */
    boolean isSelected(ProviderSession session, HubElementRef element);

    /**
     * Navigates the browser to the specified URL.
     *
     * @param session The active provider session.
     * @param url     The target URL.
     */
    void navigate(ProviderSession session, String url);

    /**
     * Navigates the browser one step back in history.
     *
     * @param session The active provider session.
     */
    void back(ProviderSession session);

    /**
     * Navigates the browser one step forward in history.
     *
     * @param session The active provider session.
     */
    void forward(ProviderSession session);

    /**
     * Refreshes the current page.
     *
     * @param session The active provider session.
     */
    void refresh(ProviderSession session);

    /**
     * Retrieves the title of the current page.
     *
     * @param session The active provider session.
     * @return The page title.
     */
    String getTitle(ProviderSession session);

    /**
     * Retrieves the current URL from the browser.
     *
     * @param session The active provider session.
     * @return The current URL.
     */
    String getCurrentUrl(ProviderSession session);

    /**
     * Retrieves the base64-encoded source code of the current page.
     *
     * @param session The active provider session.
     * @return The HTML source content.
     */
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
