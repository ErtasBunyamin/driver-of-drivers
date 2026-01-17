package com.dod.hub.provider.playwright;

import com.dod.hub.core.config.HubBrowserType;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.dod.hub.core.exception.HubTimeoutException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides an implementation of the {@link HubProvider} using the Microsoft
 * Playwright library.
 * Supports local and remote execution via browser context management.
 */
public class PlaywrightProvider implements HubProvider {

    // Internal wrapper to hold all Playwright objects
    private static class PlaywrightSessionContext {
        Playwright playwright;
        Browser browser;
        BrowserContext context;
        Page page;

        PlaywrightSessionContext(Playwright playwright, Browser browser, BrowserContext context, Page page) {
            this.playwright = playwright;
            this.browser = browser;
            this.context = context;
            this.page = page;
        }
    }

    @Override
    public String getName() {
        return "playwright";
    }

    protected Playwright createPlaywright() {
        return Playwright.create();
    }

    @Override
    public ProviderSession start(SessionCapabilities caps) {
        Playwright playwright = createPlaywright();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        options.setHeadless(caps.isHeadless());

        Browser browser;
        HubBrowserType bName = caps.getBrowserName();
        String gridUrl = caps.getGridUrl();
        boolean isRemote = gridUrl != null && !gridUrl.isEmpty();

        switch (bName) {
            case FIREFOX:
                browser = isRemote ? playwright.firefox().connect(gridUrl) : playwright.firefox().launch(options);
                break;
            case WEBKIT:
                browser = isRemote ? playwright.webkit().connect(gridUrl) : playwright.webkit().launch(options);
                break;
            case EDGE:
            case CHROME:
            default:
                browser = isRemote ? playwright.chromium().connect(gridUrl) : playwright.chromium().launch(options);
                break;
        }

        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        PlaywrightSessionContext raw = new PlaywrightSessionContext(playwright, browser, context, page);
        return new ProviderSession(getName(), caps, raw);
    }

    @Override
    public void stop(ProviderSession session) {
        PlaywrightSessionContext ctx = getCtx(session);
        if (ctx.page != null)
            ctx.page.close();
        if (ctx.context != null)
            ctx.context.close();
        if (ctx.browser != null)
            ctx.browser.close();
        if (ctx.playwright != null)
            ctx.playwright.close();
    }

    private PlaywrightSessionContext getCtx(ProviderSession session) {
        return (PlaywrightSessionContext) session.getRawDriver();
    }

    private Page getPage(ProviderSession session) {
        return getCtx(session).page;
    }

    private Locator getLocator(HubElementRef ref) {
        return (Locator) ref.getProviderHandle();
    }

    private String toSelector(HubLocator locator) {
        // Map HubLocator to Playwright selectors
        switch (locator.getStrategy()) {
            case CSS:
                return "css=" + locator.getValue();
            case XPATH:
                return "xpath=" + locator.getValue();
            case ID:
                return "#" + locator.getValue(); // Simple ID mapping
            case NAME:
                return "[name='" + locator.getValue() + "']";
            case CLASS_NAME:
                return "." + locator.getValue(); // Simple class mapping (fragile if spaces)
            case TAG_NAME:
                return "css=" + locator.getValue();
            case LINK_TEXT:
                return "text='" + locator.getValue() + "'"; // Exact text match
            case PARTIAL_LINK_TEXT:
                return "text=" + locator.getValue(); // Partial text match smart locator
            default:
                throw new IllegalArgumentException("Unsupported Locator for Playwright: " + locator.getStrategy());
        }
    }

    @Override
    public HubElementRef find(ProviderSession session, HubLocator locator) {
        Page page = getPage(session);
        // Playwright locators are lazy by default. To adhere to the HubProvider
        // contract,
        // we enforce a synchronization point by waiting for the element to be attached.
        Locator l = page.locator(toSelector(locator)).first();
        // Check for strict/eager strategy (default: true) to mimic Selenium behavior
        boolean strict = true;
        Object strictOpt = session.getCapabilities().getOptions().get("playwright.strict.find");
        if (strictOpt instanceof Boolean) {
            strict = (Boolean) strictOpt;
        } else if (strictOpt instanceof String) {
            strict = Boolean.parseBoolean((String) strictOpt);
        }

        if (strict) {
            try {
                l.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
            } catch (TimeoutError e) {
                throw new HubTimeoutException("Timed out waiting for element: " + locator, e);
            }
        }
        return new HubElementRef(locator, l);
    }

    @Override
    public List<HubElementRef> findAll(ProviderSession session, HubLocator hubLocator) {
        Page page = getPage(session);
        Locator locator = page.locator(toSelector(hubLocator));

        // Use locator.all() to retrieve all matching elements as HubElementRefs

        return locator.all().stream()
                .map(loc -> new HubElementRef(hubLocator, loc))
                .collect(Collectors.toList());
    }

    @Override
    public HubElementRef find(ProviderSession session, HubElementRef parent, HubLocator locator) {
        Locator parentLoc = getLocator(parent);
        Locator l = parentLoc.locator(toSelector(locator)).first();

        boolean strict = true;
        Object strictOpt = session.getCapabilities().getOptions().get("playwright.strict.find");
        if (strictOpt instanceof Boolean) {
            strict = (Boolean) strictOpt;
        } else if (strictOpt instanceof String) {
            strict = Boolean.parseBoolean((String) strictOpt);
        }

        if (strict) {
            try {
                l.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
            } catch (TimeoutError e) {
                throw new HubTimeoutException("Timed out waiting for element: " + locator, e);
            }
        }
        return new HubElementRef(locator, l);
    }

    @Override
    public List<HubElementRef> findAll(ProviderSession session, HubElementRef parent, HubLocator hubLocator) {
        Locator parentLoc = getLocator(parent);
        Locator locator = parentLoc.locator(toSelector(hubLocator));
        return locator.all().stream()
                .map(loc -> new HubElementRef(hubLocator, loc))
                .collect(Collectors.toList());
    }

    @Override
    public void click(ProviderSession session, HubElementRef element) {
        getLocator(element).click();
    }

    @Override
    public void type(ProviderSession session, HubElementRef element, String text) {
        getLocator(element).fill(text);
    }

    @Override
    public void clear(ProviderSession session, HubElementRef element) {
        getLocator(element).clear();
    }

    @Override
    public String getText(ProviderSession session, HubElementRef element) {
        return getLocator(element).textContent();
    }

    @Override
    public String getAttribute(ProviderSession session, HubElementRef element, String attributeName) {
        return getLocator(element).getAttribute(attributeName);
    }

    @Override
    public boolean isDisplayed(ProviderSession session, HubElementRef element) {
        return getLocator(element).isVisible();
    }

    @Override
    public boolean isEnabled(ProviderSession session, HubElementRef element) {
        return getLocator(element).isEnabled();
    }

    @Override
    public boolean isSelected(ProviderSession session, HubElementRef element) {
        return getLocator(element).isChecked();
    }

    @Override
    public void navigate(ProviderSession session, String url) {
        getPage(session).navigate(url);
    }

    @Override
    public void back(ProviderSession session) {
        getPage(session).goBack();
    }

    @Override
    public void forward(ProviderSession session) {
        getPage(session).goForward();
    }

    @Override
    public void refresh(ProviderSession session) {
        getPage(session).reload();
    }

    @Override
    public String getTitle(ProviderSession session) {
        return getPage(session).title();
    }

    @Override
    public String getCurrentUrl(ProviderSession session) {
        return getPage(session).url();
    }

    @Override
    public String getPageSource(ProviderSession session) {
        return getPage(session).content();
    }

    @Override
    public byte[] takeScreenshot(ProviderSession session) {
        return getPage(session).screenshot();
    }

    @Override
    public void setTimeouts(ProviderSession session, long implicitWaitMs, long pageLoadMs) {
        Page page = getPage(session);
        if (implicitWaitMs > 0)
            page.setDefaultTimeout((double) implicitWaitMs);
        if (pageLoadMs > 0)
            page.setDefaultNavigationTimeout((double) pageLoadMs);
    }

    // ==================== JavaScript Execution ====================

    @Override
    public Object executeScript(ProviderSession session, String script, Object... args) {
        Page page = getPage(session);
        if (args.length == 0) {
            return page.evaluate(script);
        } else if (args.length == 1) {
            return page.evaluate(script, args[0]);
        } else {
            return page.evaluate(script, Arrays.asList(args));
        }
    }

    @Override
    public Object executeAsyncScript(ProviderSession session, String script, Object... args) {
        return executeScript(session, script, args);
    }

    // ==================== Cookie Management ====================

    @Override
    public void addCookie(ProviderSession session, String name, String value, String domain, String path) {
        BrowserContext context = getCtx(session).context;
        Cookie cookie = new Cookie(name, value);
        if (domain != null)
            cookie.setDomain(domain);
        if (path != null)
            cookie.setPath(path);
        context.addCookies(Collections.singletonList(cookie));
    }

    @Override
    public void deleteCookie(ProviderSession session, String name) {
        BrowserContext context = getCtx(session).context;
        List<Cookie> cookies = context.cookies();
        context.clearCookies();
        for (Cookie c : cookies) {
            if (!c.name.equals(name)) {
                context.addCookies(Collections.singletonList(c));
            }
        }
    }

    @Override
    public void deleteAllCookies(ProviderSession session) {
        getCtx(session).context.clearCookies();
    }

    @Override
    public Set<Map<String, Object>> getCookies(ProviderSession session) {
        List<Cookie> cookies = getCtx(session).context.cookies();
        Set<Map<String, Object>> result = new HashSet<>();
        for (Cookie cookie : cookies) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", cookie.name);
            map.put("value", cookie.value);
            map.put("domain", cookie.domain);
            map.put("path", cookie.path);
            map.put("expires", cookie.expires);
            map.put("secure", cookie.secure);
            map.put("httpOnly", cookie.httpOnly);
            result.add(map);
        }
        return result;
    }

    @Override
    public Map<String, Object> getCookie(ProviderSession session, String name) {
        List<Cookie> cookies = getCtx(session).context.cookies();
        for (Cookie cookie : cookies) {
            if (cookie.name.equals(name)) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", cookie.name);
                map.put("value", cookie.value);
                map.put("domain", cookie.domain);
                map.put("path", cookie.path);
                map.put("expires", cookie.expires);
                map.put("secure", cookie.secure);
                map.put("httpOnly", cookie.httpOnly);
                return map;
            }
        }
        return null;
    }

    // ==================== Window Management ====================

    @Override
    public void maximizeWindow(ProviderSession session) {
        Page page = getPage(session);
        page.setViewportSize(1920, 1080);
    }

    @Override
    public void setWindowSize(ProviderSession session, int width, int height) {
        getPage(session).setViewportSize(width, height);
    }

    @Override
    public int[] getWindowSize(ProviderSession session) {
        Page page = getPage(session);
        Object width = page.evaluate("window.innerWidth");
        Object height = page.evaluate("window.innerHeight");
        if (width instanceof Number && height instanceof Number) {
            return new int[] { ((Number) width).intValue(), ((Number) height).intValue() };
        }
        return null;
    }

    @Override
    public int[] getWindowPosition(ProviderSession session) {
        throw new UnsupportedOperationException("getWindowPosition is not supported in Playwright");
    }

    @Override
    public void setWindowPosition(ProviderSession session, int x, int y) {
        throw new UnsupportedOperationException("setWindowPosition is not supported in Playwright");
    }

    @Override
    public void fullscreenWindow(ProviderSession session) {
        getPage(session).setViewportSize(1920, 1080);
    }

    @Override
    public void minimizeWindow(ProviderSession session) {
        throw new UnsupportedOperationException("minimizeWindow is not supported in Playwright");
    }
}
