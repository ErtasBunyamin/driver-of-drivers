package com.dod.hub.provider.hybrid;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Exposes advanced Playwright-specific capabilities that are not available in standard WebDriver.
 * <p>
 * Use this class to access Playwright's unique features while using the HybridProvider:
 * <ul>
 *   <li><strong>Auto-Wait</strong>: Smart waiting for elements to be actionable</li>
 *   <li><strong>Network Interception</strong>: Mock, modify, or block network requests</li>
 *   <li><strong>Tracing</strong>: Record traces for debugging and analysis</li>
 *   <li><strong>Video Recording</strong>: Capture test execution as video</li>
 *   <li><strong>Console Logging</strong>: Capture browser console output</li>
 *   <li><strong>Dialogs</strong>: Auto-handle JavaScript dialogs</li>
 * </ul>
 */
public class PlaywrightCapabilities {

    private final HybridSession session;
    private final Page page;

    public PlaywrightCapabilities(HybridSession session) {
        this.session = session;
        this.page = session.getPlaywrightPage();
    }

    // ==================== Auto-Wait ====================

    /**
     * Waits for an element matching the selector to be visible.
     *
     * @param selector CSS or XPath selector
     * @param timeoutMs Maximum wait time in milliseconds
     */
    public void waitForSelector(String selector, long timeoutMs) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs));
    }

    /**
     * Waits for the page to reach a specific load state.
     *
     * @param state One of: "load", "domcontentloaded", "networkidle"
     */
    public void waitForLoadState(String state) {
        switch (state.toLowerCase()) {
            case "load":
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.LOAD);
                break;
            case "domcontentloaded":
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
                break;
            case "networkidle":
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
                break;
            default:
                throw new IllegalArgumentException("Invalid load state: " + state);
        }
    }

    /**
     * Waits for a specific URL pattern to be navigated to.
     *
     * @param urlPattern Regex pattern to match URL
     */
    public void waitForURL(String urlPattern) {
        page.waitForURL(Pattern.compile(urlPattern));
    }

    // ==================== Network Interception ====================

    /**
     * Intercepts network requests matching a pattern and allows modification.
     *
     * @param urlPattern URL pattern to match (glob, regex, or predicate)
     * @param handler Handler that processes the route
     */
    public void interceptRequests(String urlPattern, Consumer<Route> handler) {
        page.route(urlPattern, handler::accept);
    }

    /**
     * Blocks all requests matching the given URL pattern.
     *
     * @param urlPattern URL pattern to block
     */
    public void blockRequests(String urlPattern) {
        page.route(urlPattern, Route::abort);
    }

    /**
     * Mocks a network request with a custom response.
     *
     * @param urlPattern URL pattern to match
     * @param body Response body to return
     * @param contentType Content-Type header value
     */
    public void mockRequest(String urlPattern, String body, String contentType) {
        page.route(urlPattern, route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setBody(body)
                    .setContentType(contentType));
        });
    }

    /**
     * Mocks a network request with JSON response.
     *
     * @param urlPattern URL pattern to match
     * @param jsonBody JSON response body
     */
    public void mockJsonRequest(String urlPattern, String jsonBody) {
        mockRequest(urlPattern, jsonBody, "application/json");
    }

    // ==================== Tracing ====================

    /**
     * Starts Playwright tracing with screenshots and snapshots.
     *
     * @param name Name for the trace
     */
    public void startTracing(String name) {
        session.getPlaywrightBrowser().contexts().get(0).tracing().start(
                new com.microsoft.playwright.Tracing.StartOptions()
                        .setName(name)
                        .setScreenshots(true)
                        .setSnapshots(true)
        );
    }

    /**
     * Stops tracing and saves to a file.
     *
     * @param outputPath Path to save the trace file (.zip)
     */
    public void stopTracing(Path outputPath) {
        session.getPlaywrightBrowser().contexts().get(0).tracing().stop(
                new com.microsoft.playwright.Tracing.StopOptions()
                        .setPath(outputPath)
        );
    }

    // ==================== Console & Errors ====================

    /**
     * Registers a handler for browser console messages.
     *
     * @param handler Consumer that receives console messages
     */
    public void onConsoleMessage(Consumer<String> handler) {
        page.onConsoleMessage(msg -> handler.accept("[" + msg.type() + "] " + msg.text()));
    }

    /**
     * Registers a handler for page errors (uncaught exceptions).
     *
     * @param handler Consumer that receives error messages
     */
    public void onPageError(Consumer<String> handler) {
        page.onPageError(handler::accept);
    }

    // ==================== Dialog Handling ====================

    /**
     * Automatically accepts all JavaScript dialogs (alert, confirm, prompt).
     */
    public void autoAcceptDialogs() {
        page.onDialog(dialog -> dialog.accept());
    }

    /**
     * Automatically dismisses all JavaScript dialogs.
     */
    public void autoDismissDialogs() {
        page.onDialog(dialog -> dialog.dismiss());
    }

    /**
     * Registers a custom dialog handler.
     *
     * @param handler Consumer that handles dialogs
     */
    public void onDialog(Consumer<com.microsoft.playwright.Dialog> handler) {
        page.onDialog(handler::accept);
    }

    // ==================== Advanced Actions ====================

    /**
     * Takes a full-page screenshot (Playwright's superior screenshot capability).
     *
     * @return Screenshot bytes
     */
    public byte[] fullPageScreenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    /**
     * Takes a screenshot of a specific element.
     *
     * @param selector Element selector
     * @return Screenshot bytes
     */
    public byte[] elementScreenshot(String selector) {
        return page.locator(selector).screenshot();
    }

    /**
     * Emulates a specific device viewport and user agent.
     *
     * @param width Viewport width
     * @param height Viewport height
     * @param deviceScaleFactor Device scale factor
     * @param isMobile Whether to emulate mobile
     */
    public void emulateDevice(int width, int height, double deviceScaleFactor, boolean isMobile) {
        page.setViewportSize(width, height);
    }

    /**
     * Sets geolocation for the page.
     *
     * @param latitude Latitude
     * @param longitude Longitude
     */
    public void setGeolocation(double latitude, double longitude) {
        session.getPlaywrightBrowser().contexts().get(0).setGeolocation(
                new com.microsoft.playwright.options.Geolocation(latitude, longitude)
        );
    }

    /**
     * Evaluates JavaScript in the page context.
     *
     * @param expression JavaScript expression
     * @return Result of evaluation
     */
    public Object evaluate(String expression) {
        return page.evaluate(expression);
    }

    /**
     * Gets the underlying Playwright Page for direct access.
     *
     * @return The Playwright Page
     */
    public Page getPage() {
        return page;
    }
}
