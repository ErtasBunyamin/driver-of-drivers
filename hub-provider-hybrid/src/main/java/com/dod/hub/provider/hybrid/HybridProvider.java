package com.dod.hub.provider.hybrid;

import com.dod.hub.core.exception.HubException;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.locator.LocatorStrategy;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.dod.hub.core.exception.HubTimeoutException;
import com.microsoft.playwright.*;
import com.microsoft.playwright.BrowserContext.WaitForPageOptions;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.ServerSocket;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * A dual-driver provider that connects both Selenium and Playwright to the same
 * browser session
 * via Chrome DevTools Protocol (CDP).
 * <p>
 * This enables leveraging the strengths of both frameworks:
 * <ul>
 * <li>Selenium: Mature element interaction, synchronous API</li>
 * <li>Playwright: Auto-waiting, network interception, modern async
 * patterns</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> This provider only supports Chromium-based browsers
 * (Chrome, Edge).
 */
public class HybridProvider implements HubProvider {

    private static final Logger logger = LoggerFactory.getLogger(HybridProvider.class);
    private static final int CDP_READY_TIMEOUT_MS = 30000;
    private static final int CDP_POLL_INTERVAL_MS = 200;
    private static final int FRAME_SYNC_TIMEOUT_MS = 2000;
    private static final long STOP_ACTION_TIMEOUT_MS = 5000;

    @Override
    public String getName() {
        return "hybrid";
    }

    @Override
    public ProviderSession start(SessionCapabilities caps) {
        String gridUrl = caps.getGridUrl();
        if (gridUrl != null && !gridUrl.isEmpty()) {
            return startRemote(caps, gridUrl);
        } else {
            return startLocal(caps);
        }
    }

    private ProviderSession startRemote(SessionCapabilities caps, String gridUrl) {
        logger.info("Starting Hybrid Session in REMOTE mode connecting to: {}", gridUrl);

        WebDriver seleniumDriver;
        try {
            seleniumDriver = createRemoteWebDriver(gridUrl, caps);
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new HubException("Invalid Grid URL: " + gridUrl, e);
        }

        String cdpUrl = null;
        boolean cdpFromOptions = false;
        if (caps.getOptions() != null && caps.getOptions().get("hybrid.cdp.url") != null) {
            cdpUrl = caps.getOptions().get("hybrid.cdp.url").toString();
            cdpFromOptions = true;
        }

        if (cdpUrl == null && seleniumDriver instanceof HasCapabilities) {
            Capabilities capabilities = ((HasCapabilities) seleniumDriver).getCapabilities();
            Object cdp = capabilities.getCapability("se:cdp");
            if (cdp != null) {
                cdpUrl = cdp.toString();
            } else {
                logger.warn("Capability 'se:cdp' not found. Remote CDP connection might fail.");
            }
        }

        if (cdpUrl == null) {
            seleniumDriver.quit();
            throw new HubException(
                    "Could not retrieve CDP endpoint. Provide 'hybrid.cdp.url' or use Selenium Grid 4 with se:cdp.");
        }

        if (!cdpFromOptions) {
            cdpUrl = sanitizeCdpUrl(cdpUrl, gridUrl);
        }

        // Resolve WebSocket URL from the HTTP endpoint if needed
        if (!cdpUrl.startsWith("ws://") && !cdpUrl.startsWith("wss://")) {
            cdpUrl = resolveCdpWebSocketUrl(cdpUrl);
        }

        logger.info("Connecting Playwright to Remote CDP: {}", cdpUrl);

        Playwright playwright = createPlaywright();
        Browser playwrightBrowser;
        try {
            playwrightBrowser = connectPlaywright(playwright, cdpUrl);
        } catch (Exception e) {
            seleniumDriver.quit();
            playwright.close();
            throw new HubException("Failed to connect Playwright to remote CDP: " + cdpUrl, e);
        }

        Page playwrightPage = playwrightBrowser.contexts().get(0).pages().get(0);

        HybridSession session = new HybridSession(
                getName(),
                caps,
                null, // No local process
                seleniumDriver,
                playwright,
                playwrightBrowser,
                playwrightPage,
                null, // No local user data dir
                cdpUrl);
        registerDialogHandler(session);
        return session;
    }

    private ProviderSession startLocal(SessionCapabilities caps) {
        int cdpPort = resolveCdpPort(caps);
        Path userDataDir = createTempProfile();

        Process browserProcess = launchBrowserWithCDP(caps, cdpPort, userDataDir);

        if (cdpPort == 0) {
            cdpPort = readDevToolsPort(userDataDir);
        }

        waitForCdpReady(cdpPort);

        String cdpUrl = resolveCdpWebSocketUrl("http://localhost:" + cdpPort);

        WebDriver seleniumDriver = connectSeleniumLocal(cdpPort, caps);

        Playwright playwright = Playwright.create();
        Browser playwrightBrowser = playwright.chromium().connectOverCDP(cdpUrl);
        Page playwrightPage = playwrightBrowser.contexts().get(0).pages().get(0);

        HybridSession session = new HybridSession(
                getName(),
                caps,
                browserProcess,
                seleniumDriver,
                playwright,
                playwrightBrowser,
                playwrightPage,
                userDataDir,
                cdpUrl);
        registerDialogHandler(session);

        logger.info("HybridSession started LOCAL on CDP port {}", cdpPort);
        return session;
    }

    @Override
    public void stop(ProviderSession session) {
        if (!(session instanceof HybridSession)) {
            throw new HubException("Expected HybridSession but got: " + session.getClass().getName());
        }
        HybridSession hybrid = (HybridSession) session;

        runWithTimeout("playwright-page-close", () -> {
            Page page = hybrid.getPlaywrightPage();
            if (page != null) {
                page.close();
            }
        });
        runWithTimeout("playwright-browser-close", () -> {
            Browser browser = hybrid.getPlaywrightBrowser();
            if (browser != null) {
                browser.close();
            }
        });
        runWithTimeout("playwright-close", () -> {
            Playwright playwright = hybrid.getPlaywright();
            if (playwright != null) {
                playwright.close();
            }
        });
        runWithTimeout("selenium-quit", () -> {
            WebDriver driver = hybrid.getSeleniumDriver();
            if (driver != null) {
                driver.quit();
            }
        });

        try {
            Process proc = hybrid.getBrowserProcess();
            if (proc != null && proc.isAlive()) {
                proc.destroy();
                if (!proc.waitFor(5, TimeUnit.SECONDS)) {
                    proc.destroyForcibly();
                    proc.waitFor(5, TimeUnit.SECONDS);
                }
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

    @Override
    public void closeWindow(ProviderSession session) {
        if (!(session instanceof HybridSession)) {
            throw new HubException("Expected HybridSession but got: " + session.getClass().getName());
        }
        HybridSession hybrid = (HybridSession) session;
        executeSelenium(hybrid, () -> hybrid.getSeleniumDriver().close());
    }

    // ==================== Element Operations (Hybrid Strategy)
    // ====================

    @Override
    public HubElementRef find(ProviderSession session, HubLocator locator) {
        HybridSession hybrid = (HybridSession) session;

        boolean usePlaywrightWait = resolveUsePlaywrightWait(hybrid.getCapabilities());

        if (usePlaywrightWait && !hybrid.hasPendingDialog() && !isAlertPresent(hybrid.getSeleniumDriver())) {
            Frame frame = hybrid.getActivePlaywrightFrame();
            String selector = toPlaywrightSelector(locator);
            try {
                Locator loc = frame.locator(selector).first();
                loc.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
            } catch (TimeoutError e) {
                logger.debug("Playwright auto-wait timed out for: {}", locator);
            } catch (PlaywrightException e) {
                logger.debug("Playwright auto-wait failed for {}: {}", locator, e.getMessage());
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
        executeSelenium((HybridSession) session, () -> ((WebElement) element.getProviderHandle()).click());
    }

    @Override
    public void type(ProviderSession session, HubElementRef element, String text) {
        executeSelenium((HybridSession) session, () -> ((WebElement) element.getProviderHandle()).sendKeys(text));
    }

    @Override
    public void clear(ProviderSession session, HubElementRef element) {
        executeSelenium((HybridSession) session, () -> ((WebElement) element.getProviderHandle()).clear());
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

    @Override
    public HubElementRef getActiveElement(ProviderSession session) {
        WebElement el = getSelenium(session).switchTo().activeElement();
        // Since we don't know the locator strategy for active element, we use a special
        // generic locator or just ID if available
        // But HubElementRef requires a locator.
        // We'll synthesise one or modify HubElementRef to allow null/unknown locator?
        // Hublocator.CSS(":active") is a reasonable approximation for the "active"
        // element concept.
        return new HubElementRef(new HubLocator(LocatorStrategy.CSS, ":active"), el);
    }

    @Override
    public void switchToWindow(ProviderSession session, String nameOrHandle) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).switchTo().window(nameOrHandle));
    }

    @Override
    public void switchToNewWindow(ProviderSession session, com.dod.hub.core.provider.HubWindowType typeHint) {
        HybridSession hybrid = (HybridSession) session;

        WindowType seleniumType = (typeHint == com.dod.hub.core.provider.HubWindowType.TAB) ? WindowType.TAB
                : WindowType.WINDOW;

        // Use Playwright's native waitForPage to capture the new window immediately
        hybrid.getPlaywrightBrowser().contexts().get(0).waitForPage(() -> {
            hybrid.getSeleniumDriver().switchTo().newWindow(seleniumType);
        });

        syncPlaywrightWindow(hybrid);
    }

    @Override
    public String getWindowHandle(ProviderSession session) {
        return ((HybridSession) session).getSeleniumDriver().getWindowHandle();
    }

    @Override
    public Set<String> getWindowHandles(ProviderSession session) {
        return ((HybridSession) session).getSeleniumDriver().getWindowHandles();
    }

    // ==================== Alert Management ====================

    @Override
    public void acceptAlert(ProviderSession session) {
        HybridSession hybrid = (HybridSession) session;
        executeSelenium(hybrid, () -> getSelenium(session).switchTo().alert().accept());
        hybrid.clearPendingDialog();
    }

    @Override
    public void dismissAlert(ProviderSession session) {
        HybridSession hybrid = (HybridSession) session;
        executeSelenium(hybrid, () -> getSelenium(session).switchTo().alert().dismiss());
        hybrid.clearPendingDialog();
    }

    @Override
    public String getAlertText(ProviderSession session) {
        HybridSession hybrid = (HybridSession) session;
        String pending = hybrid.getPendingDialogMessage();
        if (pending != null) {
            return pending;
        }
        return getSelenium(session).switchTo().alert().getText();
    }

    @Override
    public void sendKeysToAlert(ProviderSession session, String text) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).switchTo().alert().sendKeys(text));
    }

    // ==================== Capabilities ====================

    @Override
    public Map<String, Object> getCapabilities(ProviderSession session) {
        HybridSession hybridSession = (HybridSession) session;
        WebDriver driver = hybridSession.getSeleniumDriver();

        Map<String, Object> caps = new HashMap<>();
        if (driver instanceof HasCapabilities) {
            caps.putAll(((HasCapabilities) driver).getCapabilities().asMap());
        }

        if (!caps.containsKey("se:cdp") && hybridSession.getCdpUrl() != null) {
            caps.put("se:cdp", hybridSession.getCdpUrl());
            String debuggerAddr = hybridSession.getCdpUrl()
                    .replaceFirst("^wss?://", "")
                    .replaceFirst("^https?://", "");
            caps.put("goog:chromeOptions", Map.of("debuggerAddress", debuggerAddr));
        }

        return caps;
    }

    // ==================== Frame Switching ====================

    @Override
    public void switchToFrame(ProviderSession session, int index) {
        HybridSession hybrid = (HybridSession) session;

        // 1. Switch Selenium
        hybrid.getSeleniumDriver().switchTo().frame(index);

        // 2. Switch Playwright
        // Selenium's frame(0) corresponds to the first frame in the current context.
        Frame target = resolveFrameByIndex(hybrid, index);
        if (target == null) {
            throw new HubTimeoutException("Timed out waiting for Playwright frame index: " + index, null);
        }
        hybrid.setActivePlaywrightFrame(target);
    }

    @Override
    public void switchToFrame(ProviderSession session, String nameOrId) {
        HybridSession hybrid = (HybridSession) session;

        // 1. Switch Selenium
        hybrid.getSeleniumDriver().switchTo().frame(nameOrId);

        // 2. Switch Playwright
        Frame target = resolveFrameByNameOrId(hybrid, nameOrId);

        if (target != null) {
            hybrid.setActivePlaywrightFrame(target);
        } else {
            throw new HubTimeoutException("Timed out waiting for Playwright frame name/id: " + nameOrId, null);
        }
    }

    @Override
    public void switchToFrame(ProviderSession session, HubElementRef frameElement) {
        HybridSession hybrid = (HybridSession) session;
        WebElement webElement = (WebElement) frameElement.getProviderHandle();

        // 1. Switch Selenium
        hybrid.getSeleniumDriver().switchTo().frame(webElement);

        // 2. Switch Playwright
        // We need to re-locate this element in Playwright to get the frame.
        // HubElementRef holds the locator.
        HubLocator locator = frameElement.getLocator();
        if (locator != null) {
            Frame target = resolveFrameByLocator(hybrid, locator);
            if (target != null) {
                hybrid.setActivePlaywrightFrame(target);
            } else {
                throw new HubTimeoutException("Timed out waiting for Playwright frame element: " + locator, null);
            }
        } else {
            logger.warn("Frame element ref missing locator, cannot sync Playwright state.");
        }
    }

    @Override
    public void switchToParentFrame(ProviderSession session) {
        HybridSession hybrid = (HybridSession) session;

        // 1. Switch Selenium
        hybrid.getSeleniumDriver().switchTo().parentFrame();

        // 2. Switch Playwright
        Frame current = hybrid.getActivePlaywrightFrame();
        Frame parent = current.parentFrame();
        if (parent != null) {
            hybrid.setActivePlaywrightFrame(parent);
        } else {
            // Already at root?
        }
    }

    @Override
    public void switchToDefaultContent(ProviderSession session) {
        HybridSession hybrid = (HybridSession) session;

        // 1. Switch Selenium
        hybrid.getSeleniumDriver().switchTo().defaultContent();

        // 2. Switch Playwright
        hybrid.setActivePlaywrightFrame(hybrid.getPlaywrightPage().mainFrame());
    }

    // ==================== Navigation (Selenium-based) ====================

    @Override
    public void navigate(ProviderSession session, String url) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).get(url));
    }

    @Override
    public void back(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).navigate().back());
    }

    @Override
    public void forward(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).navigate().forward());
    }

    @Override
    public void refresh(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).navigate().refresh());
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

    // ==================== Screenshot (Playwright-based for higher quality)
    // ====================

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
        long scriptTimeoutMs = resolveScriptTimeoutMs(session);
        if (scriptTimeoutMs > 0) {
            driver.manage().timeouts().scriptTimeout(java.time.Duration.ofMillis(scriptTimeoutMs));
        }

        Page page = getPlaywrightPage(session);
        if (implicitWaitMs > 0)
            page.setDefaultTimeout((double) implicitWaitMs);
        if (pageLoadMs > 0)
            page.setDefaultNavigationTimeout((double) pageLoadMs);
    }

    private long resolveScriptTimeoutMs(ProviderSession session) {
        return session.getScriptTimeoutMs();
    }

    // ==================== JavaScript Execution (Selenium-based) =================

    @Override
    public Object executeScript(ProviderSession session, String script, Object... args) {
        return ((JavascriptExecutor) getSelenium(session)).executeScript(script, normalizeArgs(args));
    }

    @Override
    public Object executeAsyncScript(ProviderSession session, String script, Object... args) {
        return ((JavascriptExecutor) getSelenium(session)).executeAsyncScript(script, normalizeArgs(args));
    }

    private Object[] normalizeArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        Object[] normalized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            normalized[i] = normalizeForSelenium(args[i]);
        }
        return normalized;
    }

    private Object normalizeForSelenium(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HubElementRef) {
            return ((HubElementRef) value).getProviderHandle();
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(this::normalizeForSelenium)
                    .collect(Collectors.toList());
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> normalizeForSelenium(e.getValue())));
        }
        if (value instanceof Object[]) {
            return normalizeArgs((Object[]) value);
        }
        return value;
    }

    // ==================== Cookie Management (Selenium-based) ====================

    @Override
    public void addCookie(ProviderSession session, String name, String value, String domain, String path) {
        executeSelenium((HybridSession) session, () -> {
            Cookie.Builder builder = new Cookie.Builder(name, value);
            if (domain != null)
                builder.domain(domain);
            if (path != null)
                builder.path(path);
            getSelenium(session).manage().addCookie(builder.build());
        });
    }

    @Override
    public void deleteCookie(ProviderSession session, String name) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).manage().deleteCookieNamed(name));
    }

    @Override
    public void deleteAllCookies(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).manage().deleteAllCookies());
    }

    // ==================== Window Management ====================

    @Override
    public void maximizeWindow(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).manage().window().maximize());
    }

    @Override
    public void setWindowSize(ProviderSession session, int width, int height) {
        executeSelenium((HybridSession) session, () -> {
            getSelenium(session).manage().window().setSize(new Dimension(width, height));
            syncPlaywrightViewport(session);
        });
    }

    @Override
    public int[] getWindowSize(ProviderSession session) {
        Dimension size = getSelenium(session).manage().window().getSize();
        return new int[] { size.getWidth(), size.getHeight() };
    }

    @Override
    public int[] getWindowPosition(ProviderSession session) {
        Point pos = getSelenium(session).manage().window().getPosition();
        return new int[] { pos.getX(), pos.getY() };
    }

    @Override
    public void setWindowPosition(ProviderSession session, int x, int y) {
        executeSelenium((HybridSession) session,
                () -> getSelenium(session).manage().window().setPosition(new Point(x, y)));
    }

    @Override
    public void fullscreenWindow(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).manage().window().fullscreen());
    }

    @Override
    public void minimizeWindow(ProviderSession session) {
        executeSelenium((HybridSession) session, () -> getSelenium(session).manage().window().minimize());
    }

    @Override
    public Set<Map<String, Object>> getCookies(ProviderSession session) {
        Set<Cookie> cookies = getSelenium(session).manage().getCookies();
        Set<Map<String, Object>> result = new HashSet<>();
        for (Cookie c : cookies) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", c.getName());
            map.put("value", c.getValue());
            map.put("domain", c.getDomain());
            map.put("path", c.getPath());
            map.put("expires", c.getExpiry());
            map.put("secure", c.isSecure());
            map.put("httpOnly", c.isHttpOnly());
            result.add(map);
        }
        return result;
    }

    @Override
    public Map<String, Object> getCookie(ProviderSession session, String name) {
        Cookie c = getSelenium(session).manage().getCookieNamed(name);
        if (c == null)
            return null;
        Map<String, Object> map = new HashMap<>();
        map.put("name", c.getName());
        map.put("value", c.getValue());
        map.put("domain", c.getDomain());
        map.put("path", c.getPath());
        map.put("expires", c.getExpiry());
        map.put("secure", c.isSecure());
        map.put("httpOnly", c.isHttpOnly());
        return map;
    }

    // ==================== Internal Helpers ====================

    private WebDriver getSelenium(ProviderSession session) {
        return ((HybridSession) session).getSeleniumDriver();
    }

    private Page getPlaywrightPage(ProviderSession session) {
        return ((HybridSession) session).getPlaywrightPage();
    }

    private int resolveCdpPort(SessionCapabilities caps) {
        Object portOpt = caps.getOptions().getOrDefault("hybrid.cdp.port", "auto");
        if (portOpt instanceof Number) {
            int port = ((Number) portOpt).intValue();
            if (port == 0) {
                return 0;
            }
            if (!isPortAvailable(port)) {
                throw new HubException("CDP port " + port
                        + " is already in use. Choose a free port or set hybrid.cdp.port=auto.");
            }
            return port;
        } else if (portOpt instanceof String) {
            String raw = ((String) portOpt).trim();
            if (raw.equalsIgnoreCase("auto") || raw.equalsIgnoreCase("random") || raw.equals("0")) {
                return 0;
            }
            try {
                int port = Integer.parseInt(raw);
                if (port == 0) {
                    return 0;
                }
                if (!isPortAvailable(port)) {
                    throw new HubException("CDP port " + port
                            + " is already in use. Choose a free port or set hybrid.cdp.port=auto.");
                }
                return port;
            } catch (NumberFormatException e) {
                logger.warn("Invalid hybrid.cdp.port '{}', falling back to default/auto.", raw);
            }
        }
        return 0;
    }

    private int readDevToolsPort(Path userDataDir) {
        Path portFile = userDataDir.resolve("DevToolsActivePort");
        long deadline = System.currentTimeMillis() + CDP_READY_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            try {
                if (Files.exists(portFile)) {
                    List<String> lines = Files.readAllLines(portFile, StandardCharsets.UTF_8);
                    if (!lines.isEmpty()) {
                        String first = lines.get(0).trim();
                        if (!first.isEmpty()) {
                            return Integer.parseInt(first);
                        }
                    }
                }
            } catch (Exception e) {
                logger.trace("Waiting for DevToolsActivePort: {}", e.getMessage());
            }
            try {
                Thread.sleep(CDP_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HubException("Interrupted while waiting for DevToolsActivePort", e);
            }
        }
        throw new HubTimeoutException("DevToolsActivePort not created within " + CDP_READY_TIMEOUT_MS + "ms", null);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (IOException e) {
            return false;
        }
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
                "--remote-debugging-port=" + (cdpPort == 0 ? "0" : cdpPort),
                "--user-data-dir=" + userDataDir.toAbsolutePath(),
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-background-networking",
                "--disable-extensions",
                caps.isHeadless() ? "--headless=new" : "").stream().filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            return pb.start();
        } catch (IOException e) {
            throw new HubException("Failed to launch browser for HybridProvider", e);
        }
    }

    private String findChromePath() {
        String[] envKeys = { "CHROME_BIN", "CHROME_PATH", "GOOGLE_CHROME_BIN" };
        for (String key : envKeys) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) {
                if (new File(value).exists()) {
                    return value;
                }
            }
        }

        String os = System.getProperty("os.name").toLowerCase();
        List<String> candidates = new ArrayList<>();

        if (os.contains("win")) {
            String programFiles = System.getenv("PROGRAMFILES");
            String programFilesX86 = System.getenv("PROGRAMFILES(X86)");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (programFiles != null)
                candidates.add(programFiles + "\\Google\\Chrome\\Application\\chrome.exe");
            if (programFilesX86 != null)
                candidates.add(programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
            if (localAppData != null)
                candidates.add(localAppData + "\\Google\\Chrome\\Application\\chrome.exe");
            if (programFiles != null)
                candidates.add(programFiles + "\\Microsoft\\Edge\\Application\\msedge.exe");
            if (programFilesX86 != null)
                candidates.add(programFilesX86 + "\\Microsoft\\Edge\\Application\\msedge.exe");
        } else if (os.contains("mac")) {
            candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            candidates.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
            candidates.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
        } else {
            candidates.add("/usr/bin/google-chrome");
            candidates.add("/usr/bin/google-chrome-stable");
            candidates.add("/usr/bin/chromium");
            candidates.add("/usr/bin/chromium-browser");
            candidates.add("/snap/bin/chromium");
        }

        for (String path : candidates) {
            if (new File(path).exists()) {
                return path;
            }
        }

        throw new HubException("Chrome executable not found. Please ensure Chrome is installed or set CHROME_BIN.");
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

    private WebDriver connectSeleniumLocal(int cdpPort, SessionCapabilities caps) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "localhost:" + cdpPort);
        if (caps.getOptions() != null) {
            caps.getOptions().entrySet().stream()
                    .filter(e -> !e.getKey().startsWith("hybrid."))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .forEach(options::setCapability);
        }
        return new ChromeDriver(options);
    }

    private By toSeleniumBy(HubLocator locator) {
        switch (locator.getStrategy()) {
            case CSS:
                return By.cssSelector(locator.getValue());
            case XPATH:
                return By.xpath(locator.getValue());
            case ID:
                return By.id(locator.getValue());
            case NAME:
                return By.name(locator.getValue());
            case CLASS_NAME:
                return By.className(locator.getValue());
            case TAG_NAME:
                return By.tagName(locator.getValue());
            case LINK_TEXT:
                return By.linkText(locator.getValue());
            case PARTIAL_LINK_TEXT:
                return By.partialLinkText(locator.getValue());
            default:
                throw new IllegalArgumentException("Unsupported strategy: " + locator.getStrategy());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (path == null || !Files.exists(path))
            return;
        Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {
                    }
                });
    }

    private void runWithTimeout(String label, Runnable action) {
        Thread t = new Thread(() -> {
            try {
                action.run();
            } catch (Exception e) {
                logger.warn("Error during {}: {}", label, e.getMessage());
            }
        }, "hybrid-stop-" + label);
        t.setDaemon(true);
        t.start();
        try {
            t.join(STOP_ACTION_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) {
            logger.warn("{} did not finish within {}ms", label, STOP_ACTION_TIMEOUT_MS);
        }
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

    protected WebDriver createRemoteWebDriver(String gridUrl, SessionCapabilities caps) throws MalformedURLException {
        ChromeOptions options = new ChromeOptions();
        if (caps.getOptions() != null) {
            caps.getOptions().forEach(options::setCapability);
        }
        if (caps.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return new RemoteWebDriver(URI.create(gridUrl).toURL(), options);
    }

    protected Playwright createPlaywright() {
        return Playwright.create();
    }

    protected Browser connectPlaywright(Playwright playwright, String cdpUrl) {
        return playwright.chromium().connectOverCDP(cdpUrl);
    }

    private String toPlaywrightSelector(HubLocator locator) {
        switch (locator.getStrategy()) {
            case CSS:
                return "css=" + locator.getValue();
            case XPATH:
                return "xpath=" + locator.getValue();
            case ID:
                return "#" + locator.getValue();
            case NAME:
                return "[name='" + locator.getValue() + "']";
            case CLASS_NAME:
                return "." + locator.getValue();
            case TAG_NAME:
                return "css=" + locator.getValue();
            case LINK_TEXT:
                return "text='" + locator.getValue() + "'";
            case PARTIAL_LINK_TEXT:
                return "text=" + locator.getValue();
            default:
                throw new IllegalArgumentException("Unsupported Locator for Playwright: " + locator.getStrategy());
        }
    }

    /**
     * Replaces the host in the CDP URL with the host from the Grid URL.
     * This is necessary when the Grid returns an internal IP (e.g. Docker container
     * IP)
     * that is not reachable from the client.
     */
    protected String sanitizeCdpUrl(String cdpUrl, String gridUrl) {
        try {
            URI cdpUri = new URI(cdpUrl);
            URI gridUri = new URI(gridUrl);

            if (gridUri.getHost().equals(cdpUri.getHost())) {
                return cdpUrl;
            }

            return new URI(cdpUri.getScheme(), cdpUri.getUserInfo(), gridUri.getHost(),
                    cdpUri.getPort(), cdpUri.getPath(), cdpUri.getQuery(), cdpUri.getFragment()).toString();
        } catch (Exception e) {
            logger.warn("Failed to sanitize CDP URL: {}. Using original.", cdpUrl, e);
            return cdpUrl;
        }
    }

    /**
     * Resolves the WebSocket Debugger URL from the CDP HTTP endpoint.
     */
    private String resolveCdpWebSocketUrl(String cdpHttpUrl) {
        try {
            // Check if it's already WS/WSS
            if (cdpHttpUrl.startsWith("ws://") || cdpHttpUrl.startsWith("wss://")) {
                return cdpHttpUrl;
            }

            // Normalize HTTP URL
            String jsonUrl = cdpHttpUrl.endsWith("/") ? cdpHttpUrl + "json/version" : cdpHttpUrl + "/json/version";
            // If the input was just host:port without scheme, assume http
            if (!jsonUrl.startsWith("http")) {
                jsonUrl = "http://" + jsonUrl;
            }

            URL url = new URI(jsonUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (java.io.InputStream is = conn.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile("\"webSocketDebuggerUrl\":\\s*\"(.*?)\"").matcher(json);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
            logger.warn("Failed to fetch/parse webSocketDebuggerUrl from {}. Response code: {}", jsonUrl,
                    conn.getResponseCode());
        } catch (Exception e) {
            logger.warn("Error resolving CDP WebSocket URL from {}: {}", cdpHttpUrl, e.getMessage());
        }
        return cdpHttpUrl; // Fallback to original
    }

    private void syncPlaywrightWindow(HybridSession hybrid) {
        try {
            Browser browser = hybrid.getPlaywrightBrowser();
            BrowserContext context = browser.contexts().get(0);
            List<Page> pages = context.pages();
            if (pages.isEmpty()) {
                return;
            }
            if (pages.size() == 1) {
                Page only = pages.get(0);
                if (hybrid.getPlaywrightPage() != only && !only.isClosed()) {
                    hybrid.setPlaywrightPage(only);
                }
                return;
            }

            WebDriver selenium = hybrid.getSeleniumDriver();
            String syncId = UUID.randomUUID().toString();

            try {
                ((JavascriptExecutor) selenium).executeScript("window.__dod_sync_id = '" + syncId + "';");
            } catch (Exception e) {
                logger.trace("Could not inject sync marker (likely transitioning): {}", e.getMessage());
            }

            Page foundPage = null;
            searchLoop: for (BrowserContext ctx : browser.contexts()) {
                for (Page p : ctx.pages()) {
                    try {
                        if (p.isClosed())
                            continue;
                        Object result = p.evaluate("window.__dod_sync_id");
                        if (syncId.equals(result)) {
                            foundPage = p;
                            break searchLoop;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (foundPage != null) {
                hybrid.setPlaywrightPage(foundPage);
                try {
                    foundPage.evaluate("delete window.__dod_sync_id");
                } catch (Exception ignored) {
                }
                logger.debug("Synced Playwright window via marker: {}", syncId);
                return;
            }
            if (!pages.isEmpty()) {
                Page last = pages.get(pages.size() - 1);
                hybrid.setPlaywrightPage(last);
            }

        } catch (Exception e) {
            logger.warn("Failed to sync Playwright window in Hybrid mode", e);
        }
    }

    private void waitForPlaywrightPage(HybridSession session) {
        try {
            Browser browser = session.getPlaywrightBrowser();
            WaitForPageOptions options = new WaitForPageOptions();
            options.setTimeout(1000);
            options.setPredicate(p -> !p.url().isEmpty());
            browser.contexts().get(0).waitForPage(options, () -> {
                /* Wait for all pages to be ready */});
        } catch (Exception e) {
            logger.debug("Failed to wait for Playwright window in Hybrid mode", e);
        }
    }

    private void registerDialogHandler(HybridSession session) {
        try {
            BrowserContext ctx = session.getPlaywrightBrowser().contexts().get(0);
            ctx.onDialog(dialog -> {
                Page active = session.getPlaywrightPage();
                if (active != null && dialog.page() == active) {
                    session.setPendingDialog(dialog);
                } else {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        logger.debug("Failed to dismiss non-active dialog", e);
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to register Playwright dialog handler", e);
        }
    }

    private void executeSelenium(HybridSession session, Runnable action) {
        try {
            action.run();
        } finally {
            boolean alertPresent = isAlertPresent(session.getSeleniumDriver());
            if (!alertPresent && session.hasPendingDialog()) {
                session.clearPendingDialog();
            }
            if (!alertPresent) {
                syncPlaywrightWindow(session);
            }
        }
    }

    private <T> T handleSeleniumResult(HybridSession session, java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } finally {
            boolean alertPresent = isAlertPresent(session.getSeleniumDriver());
            if (!alertPresent && session.hasPendingDialog()) {
                session.clearPendingDialog();
            }
            if (!alertPresent) {
                syncPlaywrightWindow(session);
            }
        }
    }

    private void syncPlaywrightViewport(ProviderSession session) {
        try {
            Dimension size = getSelenium(session).manage().window().getSize();
            Thread.sleep(200);

            Object widthObj = ((JavascriptExecutor) getSelenium(session)).executeScript("return window.innerWidth;");
            Object heightObj = ((JavascriptExecutor) getSelenium(session)).executeScript("return window.innerHeight;");

            int w = Integer.parseInt(widthObj.toString());
            int h = Integer.parseInt(heightObj.toString());

            getPlaywrightPage(session).setViewportSize(w, h);
        } catch (Exception e) {
            logger.warn("Failed to sync Playwright viewport", e);
        }
    }

    private boolean isAlertPresent(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private Frame resolveFrameByIndex(HybridSession hybrid, int index) {
        Frame current = hybrid.getActivePlaywrightFrame();
        Locator loc = current.locator("iframe,frame").nth(index);
        return waitForContentFrame(loc, "index=" + index);
    }

    private Frame resolveFrameByNameOrId(HybridSession hybrid, String nameOrId) {
        Frame current = hybrid.getActivePlaywrightFrame();
        String safe = escapeCssAttr(nameOrId);
        String selector = "iframe[id=\"" + safe + "\"], frame[id=\"" + safe + "\"], " +
                "iframe[name=\"" + safe + "\"], frame[name=\"" + safe + "\"]";
        Locator loc = current.locator(selector).first();
        return waitForContentFrame(loc, "nameOrId=" + nameOrId);
    }

    private Frame resolveFrameByLocator(HybridSession hybrid, HubLocator locator) {
        Frame current = hybrid.getActivePlaywrightFrame();
        Locator loc = current.locator(toPlaywrightSelector(locator)).first();
        return waitForContentFrame(loc, "locator=" + locator);
    }

    private Frame waitForContentFrame(Locator locator, String targetLabel) {
        long deadline = System.currentTimeMillis() + FRAME_SYNC_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                ElementHandle handle = locator.elementHandle();
                if (handle != null) {
                    Frame frame = handle.contentFrame();
                    if (frame != null) {
                        return frame;
                    }
                }
            } catch (Exception e) {
                logger.debug("Frame lookup retry failed for {}: {}", targetLabel, e.getMessage());
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        logger.warn("Timed out waiting for Playwright contentFrame: {}", targetLabel);
        return null;
    }

    private String escapeCssAttr(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ==================== Actions (Interactive) ====================

    @Override
    public void performActions(ProviderSession session, Collection<?> actions) {
        // Hybrid delegates complex interactions to Selenium driver for now
        HybridSession hybrid = (HybridSession) session;
        executeSelenium(hybrid, () -> {
            WebDriver driver = hybrid.getSeleniumDriver();
            if (driver instanceof Interactive) {
                @SuppressWarnings("unchecked")
                Collection<Sequence> sequences = (Collection<Sequence>) actions;
                ((Interactive) driver).perform(sequences);
            } else {
                throw new UnsupportedOperationException(
                        "Underlying Selenium driver does not support Interactive actions");
            }
        });
    }

    @Override
    public void resetInputState(ProviderSession session) {
        HybridSession hybrid = (HybridSession) session;
        executeSelenium(hybrid, () -> {
            WebDriver driver = hybrid.getSeleniumDriver();
            if (driver instanceof Interactive) {
                ((Interactive) driver).resetInputState();
            } else {
                throw new UnsupportedOperationException(
                        "Underlying Selenium driver does not support resetting input state");
            }
        });
    }
}
