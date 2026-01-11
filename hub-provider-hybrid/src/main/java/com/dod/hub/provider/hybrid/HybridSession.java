package com.dod.hub.provider.hybrid;

import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

/**
 * A specialized session that holds references to both Selenium and Playwright driver instances,
 * connected to the same browser via Chrome DevTools Protocol (CDP).
 * <p>
 * This session enables hybrid automation strategies where each framework can be used
 * for operations where it excels.
 */
public class HybridSession extends ProviderSession {

    private final Process browserProcess;
    private final WebDriver seleniumDriver;
    private final Playwright playwright;
    private final Browser playwrightBrowser;
    private final Page playwrightPage;
    private final Path userDataDir;

    /**
     * Constructs a new HybridSession.
     *
     * @param providerName      The name of the provider ("hybrid").
     * @param caps              The session capabilities.
     * @param browserProcess    The browser OS process.
     * @param seleniumDriver    The Selenium WebDriver connected via CDP.
     * @param playwright        The Playwright instance.
     * @param playwrightBrowser The Playwright Browser connected via CDP.
     * @param playwrightPage    The active Playwright Page.
     * @param userDataDir       The temporary user data directory for the browser profile.
     */
    public HybridSession(
            String providerName,
            SessionCapabilities caps,
            Process browserProcess,
            WebDriver seleniumDriver,
            Playwright playwright,
            Browser playwrightBrowser,
            Page playwrightPage,
            Path userDataDir
    ) {
        super(providerName, caps, new DualDriverHandle(seleniumDriver, playwrightPage));
        this.browserProcess = browserProcess;
        this.seleniumDriver = seleniumDriver;
        this.playwright = playwright;
        this.playwrightBrowser = playwrightBrowser;
        this.playwrightPage = playwrightPage;
        this.userDataDir = userDataDir;
    }

    /**
     * Returns the Selenium WebDriver instance.
     *
     * @return The WebDriver connected to the shared browser session.
     */
    public WebDriver getSeleniumDriver() {
        return seleniumDriver;
    }

    /**
     * Returns the Playwright instance.
     *
     * @return The Playwright runtime instance.
     */
    public Playwright getPlaywright() {
        return playwright;
    }

    /**
     * Returns the Playwright Browser instance.
     *
     * @return The Browser connected via CDP.
     */
    public Browser getPlaywrightBrowser() {
        return playwrightBrowser;
    }

    /**
     * Returns the active Playwright Page.
     *
     * @return The Page for Playwright operations.
     */
    public Page getPlaywrightPage() {
        return playwrightPage;
    }

    /**
     * Returns the browser OS process.
     *
     * @return The Process running the browser.
     */
    public Process getBrowserProcess() {
        return browserProcess;
    }

    /**
     * Returns the temporary user data directory path.
     *
     * @return The Path to the temp profile.
     */
    public Path getUserDataDir() {
        return userDataDir;
    }

    /**
     * Returns a wrapper providing access to advanced Playwright-specific capabilities.
     * <p>
     * Use this to access features like:
     * <ul>
     *   <li>Network interception and mocking</li>
     *   <li>Auto-wait utilities</li>
     *   <li>Tracing and video recording</li>
     *   <li>Dialog handling</li>
     *   <li>Console logging</li>
     * </ul>
     *
     * @return The PlaywrightCapabilities wrapper.
     */
    public PlaywrightCapabilities playwright() {
        return new PlaywrightCapabilities(this);
    }

    /**
     * Internal holder class for dual driver references.
     */
    private static class DualDriverHandle {
        final WebDriver selenium;
        final Page playwright;

        DualDriverHandle(WebDriver selenium, Page playwright) {
            this.selenium = selenium;
            this.playwright = playwright;
        }
    }
}
