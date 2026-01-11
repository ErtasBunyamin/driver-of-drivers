package com.dod.hub.provider.hybrid;

import com.dod.hub.core.exception.HubException;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.dod.hub.core.exception.HubTimeoutException;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A dual-driver provider that connects both Selenium and Playwright to the same browser session
 * via Chrome DevTools Protocol (CDP).
 * <p>
 * This enables leveraging the strengths of both frameworks:
 * <ul>
 *   <li>Selenium: Mature element interaction, synchronous API</li>
 *   <li>Playwright: Auto-waiting, network interception, modern async patterns</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> This provider only supports Chromium-based browsers (Chrome, Edge).
 */
public class HybridProvider implements HubProvider {

    private static final Logger logger = LoggerFactory.getLogger(HybridProvider.class);
    private static final int DEFAULT_CDP_PORT = 9222;
    private static final int CDP_READY_TIMEOUT_MS = 10000;
    private static final int CDP_POLL_INTERVAL_MS = 200;

    @Override
    public String getName() {
        return "hybrid";
    }

    @Override
    public ProviderSession start(SessionCapabilities caps) {
        int cdpPort = resolveCdpPort(caps);
        Path userDataDir = createTempProfile();

        Process browserProcess = launchBrowserWithCDP(caps, cdpPort, userDataDir);

        waitForCdpReady(cdpPort);

        WebDriver seleniumDriver = connectSelenium(cdpPort, caps);

        Playwright playwright = Playwright.create();
        Browser playwrightBrowser = playwright.chromium().connectOverCDP("http://localhost:" + cdpPort);
        Page playwrightPage = playwrightBrowser.contexts().get(0).pages().get(0);

        HybridSession session = new HybridSession(
                getName(),
                caps,
                browserProcess,
                seleniumDriver,
                playwright,
                playwrightBrowser,
                playwrightPage,
                userDataDir
        );

        logger.info("HybridSession started on CDP port {}", cdpPort);
        return session;
    }

    @Override
    public void stop(ProviderSession session) {
        if (!(session instanceof HybridSession)) {
            throw new HubException("Expected HybridSession but got: " + session.getClass().getName());
        }
        HybridSession hybrid = (HybridSession) session;

        try {
            if (hybrid.getPlaywrightPage() != null) hybrid.getPlaywrightPage().close();
            if (hybrid.getPlaywrightBrowser() != null) hybrid.getPlaywrightBrowser().close();
            if (hybrid.getPlaywright() != null) hybrid.getPlaywright().close();
        } catch (Exception e) {
            logger.warn("Error closing Playwright resources", e);
        }

        try {
            if (hybrid.getSeleniumDriver() != null) hybrid.getSeleniumDriver().quit();
        } catch (Exception e) {
            logger.warn("Error closing Selenium driver", e);
        }

        try {
            Process proc = hybrid.getBrowserProcess();
            if (proc != null && proc.isAlive()) {
                proc.destroy();
                proc.waitFor();
            }
        } catch (Exception e) {
            logger.warn("Error terminating browser process", e);
        }

        try {
            deleteDirectory(hybrid.getUserDataDir());
        } catch (Exception e) {
            logger.warn("Failed to cleanup temp profile: {}", hybrid.getUserDataDir(), e);
        }

        logger.info("HybridSession stopped");
    }

    // ==================== Element Operations (Hybrid Strategy) ====================

    @Override
    public HubElementRef find(ProviderSession session, HubLocator locator) {
        HybridSession hybrid = (HybridSession) session;
        
        boolean usePlaywrightWait = resolveUsePlaywrightWait(hybrid.getCapabilities());
        
        if (usePlaywrightWait) {
            Page page = hybrid.getPlaywrightPage();
            String selector = toPlaywrightSelector(locator);
            try {
                Locator loc = page.locator(selector).first();
                loc.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            } catch (TimeoutError e) {
                throw new HubTimeoutException("Playwright auto-wait timed out for: " + locator, e);
            }
        }
        
        WebDriver driver = hybrid.getSeleniumDriver();
        try {
            WebElement el = driver.findElement(toSeleniumBy(locator));
            return new HubElementRef(locator, el);
        } catch (TimeoutException e) {
            throw new HubTimeoutException("Timed out waiting for element: " + locator, e);
        } catch (NoSuchElementException e) {
            throw new HubException("Element not found: " + locator, e);
        }
    }

    @Override
    public List<HubElementRef> findAll(ProviderSession session, HubLocator locator) {
        WebDriver driver = getSelenium(session);
        List<WebElement> els = driver.findElements(toSeleniumBy(locator));
        return els.stream()
                .map(el -> new HubElementRef(locator, el))
                .collect(Collectors.toList());
    }

    @Override
    public HubElementRef find(ProviderSession session, HubElementRef parent, HubLocator locator) {
        WebElement parentEl = (WebElement) parent.getProviderHandle();
        try {
            WebElement el = parentEl.findElement(toSeleniumBy(locator));
            return new HubElementRef(locator, el);
        } catch (TimeoutException e) {
            throw new HubTimeoutException("Timed out waiting for element: " + locator, e);
        } catch (NoSuchElementException e) {
            throw new HubException("Element not found: " + locator, e);
        }
    }

    @Override
    public List<HubElementRef> findAll(ProviderSession session, HubElementRef parent, HubLocator locator) {
        WebElement parentEl = (WebElement) parent.getProviderHandle();
        List<WebElement> els = parentEl.findElements(toSeleniumBy(locator));
        return els.stream()
                .map(el -> new HubElementRef(locator, el))
                .collect(Collectors.toList());
    }

    @Override
    public void click(ProviderSession session, HubElementRef element) {
        ((WebElement) element.getProviderHandle()).click();
    }

    @Override
    public void type(ProviderSession session, HubElementRef element, String text) {
        ((WebElement) element.getProviderHandle()).sendKeys(text);
    }

    @Override
    public void clear(ProviderSession session, HubElementRef element) {
        ((WebElement) element.getProviderHandle()).clear();
    }

    @Override
    public String getText(ProviderSession session, HubElementRef element) {
        return ((WebElement) element.getProviderHandle()).getText();
    }

    @Override
    public String getAttribute(ProviderSession session, HubElementRef element, String attributeName) {
        return ((WebElement) element.getProviderHandle()).getAttribute(attributeName);
    }

    @Override
    public boolean isDisplayed(ProviderSession session, HubElementRef element) {
        return ((WebElement) element.getProviderHandle()).isDisplayed();
    }

    @Override
    public boolean isEnabled(ProviderSession session, HubElementRef element) {
        return ((WebElement) element.getProviderHandle()).isEnabled();
    }

    @Override
    public boolean isSelected(ProviderSession session, HubElementRef element) {
        return ((WebElement) element.getProviderHandle()).isSelected();
    }

    // ==================== Navigation (Selenium-based) ====================

    @Override
    public void navigate(ProviderSession session, String url) {
        getSelenium(session).get(url);
    }

    @Override
    public void back(ProviderSession session) {
        getSelenium(session).navigate().back();
    }

    @Override
    public void forward(ProviderSession session) {
        getSelenium(session).navigate().forward();
    }

    @Override
    public void refresh(ProviderSession session) {
        getSelenium(session).navigate().refresh();
    }

    @Override
    public String getTitle(ProviderSession session) {
        return getSelenium(session).getTitle();
    }

    @Override
    public String getCurrentUrl(ProviderSession session) {
        return getSelenium(session).getCurrentUrl();
    }

    @Override
    public String getPageSource(ProviderSession session) {
        return getSelenium(session).getPageSource();
    }

    // ==================== Screenshot (Playwright-based for higher quality) ====================

    @Override
    public byte[] takeScreenshot(ProviderSession session) {
        Page page = getPlaywrightPage(session);
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    @Override
    public void setTimeouts(ProviderSession session, long implicitWaitMs, long pageLoadMs) {
        WebDriver driver = getSelenium(session);
        if (implicitWaitMs > 0)
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(implicitWaitMs));
        if (pageLoadMs > 0)
            driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofMillis(pageLoadMs));

        Page page = getPlaywrightPage(session);
        if (implicitWaitMs > 0) page.setDefaultTimeout((double) implicitWaitMs);
        if (pageLoadMs > 0) page.setDefaultNavigationTimeout((double) pageLoadMs);
    }

    // ==================== Internal Helpers ====================

    private WebDriver getSelenium(ProviderSession session) {
        return ((HybridSession) session).getSeleniumDriver();
    }

    private Page getPlaywrightPage(ProviderSession session) {
        return ((HybridSession) session).getPlaywrightPage();
    }

    private int resolveCdpPort(SessionCapabilities caps) {
        Object portOpt = caps.getOptions().get("hybrid.cdp.port");
        if (portOpt instanceof Number) {
            return ((Number) portOpt).intValue();
        } else if (portOpt instanceof String) {
            return Integer.parseInt((String) portOpt);
        }
        return DEFAULT_CDP_PORT;
    }

    private Path createTempProfile() {
        try {
            return Files.createTempDirectory("hub-hybrid-profile-");
        } catch (IOException e) {
            throw new HubException("Failed to create temp profile directory", e);
        }
    }

    private Process launchBrowserWithCDP(SessionCapabilities caps, int cdpPort, Path userDataDir) {
        String chromePath = findChromePath();
        List<String> command = List.of(
                chromePath,
                "--remote-debugging-port=" + cdpPort,
                "--user-data-dir=" + userDataDir.toAbsolutePath(),
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-background-networking",
                "--disable-extensions",
                caps.isHeadless() ? "--headless=new" : ""
        ).stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            return pb.start();
        } catch (IOException e) {
            throw new HubException("Failed to launch browser for HybridProvider", e);
        }
    }

    private String findChromePath() {
        String[] windowsPaths = {
                System.getenv("PROGRAMFILES") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("PROGRAMFILES(X86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe"
        };
        for (String path : windowsPaths) {
            if (new File(path).exists()) return path;
        }
        throw new HubException("Chrome executable not found. Please ensure Chrome is installed.");
    }

    private void waitForCdpReady(int port) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < CDP_READY_TIMEOUT_MS) {
            try {
                URL url = new URL("http://localhost:" + port + "/json/version");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CDP_POLL_INTERVAL_MS);
                conn.setReadTimeout(CDP_POLL_INTERVAL_MS);
                if (conn.getResponseCode() == 200) {
                    logger.debug("CDP ready on port {}", port);
                    return;
                }
            } catch (Exception ignored) {
                // Not ready yet
            }
            try {
                Thread.sleep(CDP_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HubException("Interrupted while waiting for CDP", e);
            }
        }
        throw new HubTimeoutException("CDP did not become ready within " + CDP_READY_TIMEOUT_MS + "ms", null);
    }

    private WebDriver connectSelenium(int cdpPort, SessionCapabilities caps) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "localhost:" + cdpPort);
        if (caps.getOptions() != null) {
            caps.getOptions().forEach(options::setCapability);
        }
        return new ChromeDriver(options);
    }

    private By toSeleniumBy(HubLocator locator) {
        switch (locator.getStrategy()) {
            case CSS: return By.cssSelector(locator.getValue());
            case XPATH: return By.xpath(locator.getValue());
            case ID: return By.id(locator.getValue());
            case NAME: return By.name(locator.getValue());
            case CLASS_NAME: return By.className(locator.getValue());
            case TAG_NAME: return By.tagName(locator.getValue());
            case LINK_TEXT: return By.linkText(locator.getValue());
            case PARTIAL_LINK_TEXT: return By.partialLinkText(locator.getValue());
            default: throw new IllegalArgumentException("Unsupported strategy: " + locator.getStrategy());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
    }

    private boolean resolveUsePlaywrightWait(SessionCapabilities caps) {
        Object opt = caps.getOptions().get("hybrid.playwright.autowait");
        if (opt instanceof Boolean) {
            return (Boolean) opt;
        } else if (opt instanceof String) {
            return Boolean.parseBoolean((String) opt);
        }
        return true; // Default: enabled
    }

    private String toPlaywrightSelector(HubLocator locator) {
        switch (locator.getStrategy()) {
            case CSS: return "css=" + locator.getValue();
            case XPATH: return "xpath=" + locator.getValue();
            case ID: return "#" + locator.getValue();
            case NAME: return "[name='" + locator.getValue() + "']";
            case CLASS_NAME: return "." + locator.getValue();
            case TAG_NAME: return "css=" + locator.getValue();
            case LINK_TEXT: return "text='" + locator.getValue() + "'";
            case PARTIAL_LINK_TEXT: return "text=" + locator.getValue();
            default: throw new IllegalArgumentException("Unsupported Locator for Playwright: " + locator.getStrategy());
        }
    }
}
