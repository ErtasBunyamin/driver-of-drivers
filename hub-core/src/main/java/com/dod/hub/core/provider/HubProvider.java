package com.dod.hub.core.provider;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // ==================== JavaScript Execution ====================

    /**
     * Executes JavaScript in the context of the current page.
     *
     * @param session The active provider session.
     * @param script  The JavaScript code to execute.
     * @param args    Arguments to pass to the script.
     * @return The result of the script execution.
     */
    default Object executeScript(ProviderSession session, String script, Object... args) {
        throw new UnsupportedOperationException("JavaScript execution is not supported by this provider.");
    }

    /**
     * Executes asynchronous JavaScript in the context of the current page.
     *
     * @param session The active provider session.
     * @param script  The JavaScript code to execute.
     * @param args    Arguments to pass to the script.
     * @return The result of the script execution.
     */
    default Object executeAsyncScript(ProviderSession session, String script, Object... args) {
        throw new UnsupportedOperationException("Async JavaScript execution is not supported by this provider.");
    }

    // ==================== Cookie Management ====================

    /**
     * Adds a cookie to the current session.
     *
     * @param session The active provider session.
     * @param name    Cookie name.
     * @param value   Cookie value.
     * @param domain  Cookie domain (can be null).
     * @param path    Cookie path (can be null).
     */
    default void addCookie(ProviderSession session, String name, String value, String domain, String path) {
        // Default no-op for backward compatibility
    }

    /**
     * Deletes a cookie by name.
     *
     * @param session The active provider session.
     * @param name    The name of the cookie to delete.
     */
    default void deleteCookie(ProviderSession session, String name) {
        // Default no-op for backward compatibility
    }

    /**
     * Deletes all cookies from the current session.
     *
     * @param session The active provider session.
     */
    default void deleteAllCookies(ProviderSession session) {
        // Default no-op for backward compatibility
    }

    /**
     * Retrieves all cookies from the current session.
     *
     * @param session The active provider session.
     * @return A set of cookie maps containing name, value, domain, path, etc.
     */
    default Set<Map<String, Object>> getCookies(ProviderSession session) {
        return Collections.emptySet();
    }

    /**
     * Retrieves a cookie by name.
     *
     * @param session The active provider session.
     * @param name    The name of the cookie.
     * @return Cookie data as a map, or null if not found.
     */
    default Map<String, Object> getCookie(ProviderSession session, String name) {
        return null;
    }

    // ==================== Window Management ====================

    /**
     * Maximizes the browser window.
     *
     * @param session The active provider session.
     */
    default void maximizeWindow(ProviderSession session) {
        // Default no-op for backward compatibility
    }

    /**
     * Sets the browser window size.
     *
     * @param session The active provider session.
     * @param width   Window width in pixels.
     * @param height  Window height in pixels.
     */
    default void setWindowSize(ProviderSession session, int width, int height) {
        // Default no-op for backward compatibility
    }

    /**
     * Gets the browser window size.
     *
     * @param session The active provider session.
     * @return An int array [width, height], or null if not supported.
     */
    default int[] getWindowSize(ProviderSession session) {
        return null;
    }

    /**
     * Gets the browser window position.
     *
     * @param session The active provider session.
     * @return An int array [x, y], or null if not supported.
     */
    default int[] getWindowPosition(ProviderSession session) {
        return null;
    }

    /**
     * Sets the browser window position.
     *
     * @param session The active provider session.
     * @param x       X coordinate.
     * @param y       Y coordinate.
     */
    default void setWindowPosition(ProviderSession session, int x, int y) {
        // Default no-op for backward compatibility
    }

    /**
     * Fullscreens the browser window.
     *
     * @param session The active provider session.
     */
    default void fullscreenWindow(ProviderSession session) {
        // Default no-op for backward compatibility
    }

    /**
     * Minimizes the browser window.
     *
     * @param session The active provider session.
     */
    default void minimizeWindow(ProviderSession session) {
        // Default no-op for backward compatibility
    }
}
