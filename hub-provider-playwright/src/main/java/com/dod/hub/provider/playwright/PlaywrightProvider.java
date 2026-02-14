package com.dod.hub.provider.playwright;

import com.dod.hub.core.config.HubBrowserType;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.dod.hub.core.exception.HubTimeoutException;

import java.net.ServerSocket;
import java.util.*;
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
        Frame activeFrame;
        volatile Dialog pendingDialog;
        Map<String, Page> handleMap = new HashMap<>();
        String cdpUrl;
        Process chromiumProcess;

        PlaywrightSessionContext(Playwright playwright, Browser browser, BrowserContext context, Page page) {
            this(playwright, browser, context, page, null);
        }

        PlaywrightSessionContext(Playwright playwright, Browser browser, BrowserContext context, Page page,
                String cdpUrl) {
            this.playwright = playwright;
            this.browser = browser;
            this.context = context;
            this.page = page;
            this.activeFrame = page.mainFrame();
            this.cdpUrl = cdpUrl;
            registerPage(page);

            // Automatically register new pages (popups, target="_blank", etc.)
            context.onPage(p -> registerPage(p));
            context.onDialog(d -> this.pendingDialog = d);
        }

        String registerPage(Page p) {
            // Check if page is already registered
            for (Map.Entry<String, Page> entry : handleMap.entrySet()) {
                if (entry.getValue() == p) {
                    return entry.getKey();
                }
            }
            String handle = UUID.randomUUID().toString().toUpperCase().replace("-", "");
            handleMap.put(handle, p);
            return handle;
        }

        String getHandle(Page p) {
            for (Map.Entry<String, Page> entry : handleMap.entrySet()) {
                if (entry.getValue() == p) {
                    return entry.getKey();
                }
            }
            return registerPage(p);
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
        boolean isWs = isRemote && (gridUrl.startsWith("ws://") || gridUrl.startsWith("wss://"));
        int debugPort = 0;
        Process chromiumProcess = null;

        switch (bName) {
            case FIREFOX:
                if (isRemote) {
                    if (!isWs) {
                        throw new com.dod.hub.core.exception.HubException(
                                "Remote Firefox requires a Playwright WS endpoint (ws:// or wss://).");
                    }
                    browser = playwright.firefox().connect(gridUrl);
                } else {
                    browser = playwright.firefox().launch(options);
                }
                break;
            case WEBKIT:
                if (isRemote) {
                    if (!isWs) {
                        throw new com.dod.hub.core.exception.HubException(
                                "Remote WebKit requires a Playwright WS endpoint (ws:// or wss://).");
                    }
                    browser = playwright.webkit().connect(gridUrl);
                } else {
                    browser = playwright.webkit().launch(options);
                }
                break;
            case EDGE:
            case CHROME:
            default:
                if (isRemote) {
                    if (isWs) {
                        browser = playwright.chromium().connect(gridUrl);
                    } else {
                        browser = playwright.chromium().connectOverCDP(gridUrl);
                    }
                } else {
                    // Self-launch Chromium with a debugging port to expose se:cdp
                    debugPort = findFreePort();
                    if (debugPort > 0) {
                        chromiumProcess = launchChromium(playwright, caps.isHeadless(), debugPort);
                        String localCdp = discoverCdpUrl(debugPort);
                        if (localCdp != null) {
                            browser = playwright.chromium().connectOverCDP(
                                    "http://127.0.0.1:" + debugPort);
                        } else {
                            // Fallback: if self-launch fails, use regular launch
                            if (chromiumProcess != null) {
                                chromiumProcess.destroyForcibly();
                                chromiumProcess = null;
                            }
                            browser = playwright.chromium().launch(options);
                        }
                    } else {
                        browser = playwright.chromium().launch(options);
                    }
                }
                break;
        }

        String cdpUrl = null;
        if (isRemote && !isWs && (bName == HubBrowserType.CHROME || bName == HubBrowserType.EDGE)) {
            cdpUrl = gridUrl;
        } else if (!isRemote && (bName == HubBrowserType.CHROME || bName == HubBrowserType.EDGE)
                && debugPort > 0 && chromiumProcess != null) {
            cdpUrl = discoverCdpUrl(debugPort);
        }

        BrowserContext context;
        if (browser.contexts().isEmpty()) {
            context = browser.newContext();
        } else {
            context = browser.contexts().get(0);
        }
        Page page;
        if (context.pages().isEmpty()) {
            page = context.newPage();
        } else {
            page = context.pages().get(0);
        }

        PlaywrightSessionContext raw = new PlaywrightSessionContext(playwright, browser, context, page, cdpUrl);
        raw.chromiumProcess = chromiumProcess;
        return new ProviderSession(getName(), caps, raw);
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            return 0;
        }
    }

    private static Process launchChromium(Playwright playwright, boolean headless, int port) {
        try {
            String execPath = playwright.chromium().executablePath();
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("pw-cdp-");
            List<String> cmd = new ArrayList<>();
            cmd.add(execPath);
            cmd.add("--remote-debugging-port=" + port);
            cmd.add("--user-data-dir=" + tempDir.toAbsolutePath());
            cmd.add("--no-first-run");
            cmd.add("--no-default-browser-check");
            cmd.add("--disable-background-networking");
            cmd.add("--disable-default-apps");
            if (headless) {
                cmd.add("--headless=new");
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (Exception e) {
            return null;
        }
    }

    private static String discoverCdpUrl(int port) {
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                java.net.URL url = java.net.URI.create(
                        "http://127.0.0.1:" + port + "/json/version").toURL();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                if (conn.getResponseCode() == 200) {
                    try (java.io.InputStream is = conn.getInputStream()) {
                        String body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        // Parse webSocketDebuggerUrl from JSON
                        int idx = body.indexOf("\"webSocketDebuggerUrl\"");
                        if (idx >= 0) {
                            int colon = body.indexOf(':', idx + 21);
                            int quote1 = body.indexOf('"', colon + 1);
                            int quote2 = body.indexOf('"', quote1 + 1);
                            if (quote1 >= 0 && quote2 > quote1) {
                                return body.substring(quote1 + 1, quote2);
                            }
                        }
                        return "ws://127.0.0.1:" + port + "/devtools/browser";
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
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
        if (ctx.chromiumProcess != null) {
            ctx.chromiumProcess.destroyForcibly();
        }
    }

    @Override
    public void closeWindow(ProviderSession session) {
        PlaywrightSessionContext ctx = getCtx(session);
        if (ctx.page != null && !ctx.page.isClosed()) {
            ctx.page.close();
        }
        ctx.handleMap.values().removeIf(Page::isClosed);
        if (!ctx.handleMap.isEmpty()) {
            Page next = ctx.handleMap.values().iterator().next();
            ctx.page = next;
            ctx.activeFrame = next.mainFrame();
        }
    }

    private PlaywrightSessionContext getCtx(ProviderSession session) {
        return (PlaywrightSessionContext) session.getRawDriver();
    }

    private Page getPage(ProviderSession session) {
        return getCtx(session).page;
    }

    private Frame getActiveFrame(ProviderSession session) {
        PlaywrightSessionContext ctx = getCtx(session);
        return ctx.activeFrame != null ? ctx.activeFrame : ctx.page.mainFrame();
    }

    private void setActiveFrame(ProviderSession session, Frame frame) {
        getCtx(session).activeFrame = frame;
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

    // ==================== Frame Switching ====================

    @Override
    public void switchToFrame(ProviderSession session, int index) {
        Frame current = getActiveFrame(session);
        Locator loc = current.locator("iframe,frame").nth(index);
        Frame target = resolveFrame(loc);
        if (target == null) {
            throw new HubTimeoutException("Timed out waiting for frame index: " + index, null);
        }
        setActiveFrame(session, target);
    }

    @Override
    public void switchToFrame(ProviderSession session, String nameOrId) {
        Frame current = getActiveFrame(session);
        String safe = nameOrId.replace("\\", "\\\\").replace("\"", "\\\"");
        String selector = "iframe[id=\"" + safe + "\"], frame[id=\"" + safe + "\"], " +
                "iframe[name=\"" + safe + "\"], frame[name=\"" + safe + "\"]";
        Locator loc = current.locator(selector).first();
        Frame target = resolveFrame(loc);
        if (target == null) {
            throw new HubTimeoutException("Timed out waiting for frame name/id: " + nameOrId, null);
        }
        setActiveFrame(session, target);
    }

    @Override
    public void switchToFrame(ProviderSession session, HubElementRef frameElement) {
        Locator locator = getLocator(frameElement);
        Frame target = resolveFrame(locator);
        if (target == null) {
            throw new HubTimeoutException("Timed out waiting for frame element: " + frameElement.getLocator(), null);
        }
        setActiveFrame(session, target);
    }

    @Override
    public void switchToParentFrame(ProviderSession session) {
        Frame current = getActiveFrame(session);
        Frame parent = current.parentFrame();
        if (parent != null) {
            setActiveFrame(session, parent);
        }
    }

    @Override
    public void switchToDefaultContent(ProviderSession session) {
        setActiveFrame(session, getPage(session).mainFrame());
    }

    @Override
    public HubElementRef getActiveElement(ProviderSession session) {
        Frame frame = getActiveFrame(session);
        Locator loc = frame.locator(":focus").first();
        return new HubElementRef(new HubLocator(com.dod.hub.core.locator.LocatorStrategy.CSS, ":focus"), loc);
    }

    private Frame resolveFrame(Locator locator) {
        try {
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
            ElementHandle handle = locator.elementHandle();
            if (handle != null) {
                return handle.contentFrame();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // ==================== Alert Management ====================

    @Override
    public void acceptAlert(ProviderSession session) {
        Dialog dialog = getCtx(session).pendingDialog;
        if (dialog == null) {
            throw new com.dod.hub.core.exception.HubException("No alert present");
        }
        dialog.accept();
        getCtx(session).pendingDialog = null;
    }

    @Override
    public void dismissAlert(ProviderSession session) {
        Dialog dialog = getCtx(session).pendingDialog;
        if (dialog == null) {
            throw new com.dod.hub.core.exception.HubException("No alert present");
        }
        dialog.dismiss();
        getCtx(session).pendingDialog = null;
    }

    @Override
    public String getAlertText(ProviderSession session) {
        Dialog dialog = getCtx(session).pendingDialog;
        if (dialog == null) {
            throw new com.dod.hub.core.exception.HubException("No alert present");
        }
        return dialog.message();
    }

    @Override
    public void sendKeysToAlert(ProviderSession session, String text) {
        Dialog dialog = getCtx(session).pendingDialog;
        if (dialog == null) {
            throw new com.dod.hub.core.exception.HubException("No alert present");
        }
        dialog.accept(text);
        getCtx(session).pendingDialog = null;
    }

    @Override
    public HubElementRef find(ProviderSession session, HubLocator locator) {
        Frame frame = getActiveFrame(session);
        // Playwright locators are lazy by default. To adhere to the HubProvider
        // contract,
        // we enforce a synchronization point by waiting for the element to be attached.
        Locator l = frame.locator(toSelector(locator)).first();
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
        Frame frame = getActiveFrame(session);
        Locator locator = frame.locator(toSelector(hubLocator));

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
        getLocator(element).type(text);
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
        Frame frame = getActiveFrame(session);
        List<Object> normalizedArgs = normalizeArgs(args);

        // Wrap the user's script in a function that provides 'arguments'
        // We use 'new Function' inside evaluate to create a scope where 'arguments' is
        // available
        // and mapped from the passed args.
        String wrapper = ""
                + "(args) => {\n"
                + "  const __userScript = function() { \n"
                + "      // Map args to arguments\n"
                + "      const arguments = args;\n"
                + "      " + script + "\n"
                + "  };\n"
                + "  return __userScript.apply(null, args);\n"
                + "}";

        try {
            return frame.evaluate(wrapper, normalizedArgs);
        } catch (PlaywrightException e) {
            // Fallback: fast path if the script is simple return or doesn't use arguments
            // This might not be needed if wrapper is robust, but good for debugging if
            // wrapper fails
            throw e;
        }
    }

    @Override
    public Object executeAsyncScript(ProviderSession session, String script, Object... args) {
        Frame frame = getActiveFrame(session);
        long timeoutMs = resolveScriptTimeoutMs(session);

        Map<String, Object> payload = new HashMap<>();
        payload.put("script", script);
        payload.put("args", normalizeArgs(args));
        payload.put("timeoutMs", timeoutMs > 0 ? (double) timeoutMs : 0d);

        String wrapper = ""
                + "(payload) => {\n"
                + "  const script = payload.script || \"\";\n"
                + "  const args = Array.isArray(payload.args) ? payload.args : [];\n"
                + "  const timeoutMs = payload.timeoutMs || 0;\n"
                + "  return new Promise((resolve, reject) => {\n"
                + "    let done = false;\n"
                + "    let timer = null;\n"
                + "    if (timeoutMs > 0) {\n"
                + "      timer = setTimeout(() => {\n"
                + "        if (done) return;\n"
                + "        done = true;\n"
                + "        reject('Script timeout after ' + timeoutMs + 'ms');\n"
                + "      }, timeoutMs);\n"
                + "    }\n"
                + "    const callback = (...cbArgs) => {\n"
                + "      if (done) return;\n"
                + "      done = true;\n"
                + "      if (timer) clearTimeout(timer);\n"
                + "      resolve(cbArgs.length <= 1 ? cbArgs[0] : cbArgs);\n"
                + "    };\n"
                + "    try {\n"
                + "      // Wrap in function with 'arguments'\n"
                + "      const fn = new Function('args', 'callback',\n"
                + "        'const arguments = Array.isArray(args) ? args.slice() : [];'\n"
                + "        + ' arguments.push(callback);'\n"
                + "        + ' return (function() { ' + script + ' }).apply(null, arguments);'\n"
                + "      );\n"
                + "      fn(args, callback);\n"
                + "    } catch (e) {\n"
                + "      if (timer) clearTimeout(timer);\n"
                + "      reject(e && e.message ? e.message : String(e));\n"
                + "    }\n"
                + "  });\n"
                + "}";

        try {
            return frame.evaluate(wrapper, payload);
        } catch (PlaywrightException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("Script timeout")) {
                throw new HubTimeoutException(msg, e);
            }
            throw e;
        }
    }

    private long resolveScriptTimeoutMs(ProviderSession session) {
        return session.getScriptTimeoutMs();
    }

    private List<Object> normalizeArgs(Object[] args) {
        List<Object> normalized = new ArrayList<>(args.length);
        for (Object arg : args) {
            normalized.add(normalizeForPlaywright(arg));
        }
        return normalized;
    }

    private Object normalizeForPlaywright(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HubElementRef) {
            return getLocator((HubElementRef) value).elementHandle();
        }
        if (value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) {
                normalized.add(normalizeForPlaywright(item));
            }
            return normalized;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                normalized.put(key, normalizeForPlaywright(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof Object[]) {
            return normalizeArgs((Object[]) value);
        }
        return value.toString();
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
            map.put("expiry", toExpiryDate(cookie.expires));
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
                map.put("expiry", toExpiryDate(cookie.expires));
                map.put("secure", cookie.secure);
                map.put("httpOnly", cookie.httpOnly);
                return map;
            }
        }
        return null;
    }

    private Date toExpiryDate(double expiresSeconds) {
        if (expiresSeconds <= 0) {
            return null;
        }
        long millis = (long) (expiresSeconds * 1000);
        return new Date(millis);
    }

    // ==================== Window Management ====================

    @Override
    public void maximizeWindow(ProviderSession session) {
        Page page = getPage(session);
        int[] size = getScreenSize(page);
        page.setViewportSize(size[0], size[1]);
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
        PlaywrightSessionContext ctx = getCtx(session);
        Map<String, JsonElement> bounds = resolveWindowBounds(ctx);
        if (bounds != null) {
            int left = toInt(bounds.get("left"));
            int top = toInt(bounds.get("top"));
            return new int[] { left, top };
        }
        try {
            Object result = ctx.page.evaluate("() => [window.screenX || 0, window.screenY || 0]");
            if (result instanceof List) {
                List list = (List) result;
                if (list.size() >= 2) {
                    return new int[] { toInt(list.get(0)), toInt(list.get(1)) };
                }
            }
        } catch (Exception ignored) {
        }
        throw new UnsupportedOperationException("Window position requires a Chromium CDP session.");
    }

    @Override
    public void setWindowPosition(ProviderSession session, int x, int y) {
        Map<String, Object> bounds = new HashMap<>();
        bounds.put("left", x);
        bounds.put("top", y);
        bounds.put("windowState", "normal");
        setWindowBounds(session, bounds);
    }

    @Override
    public void fullscreenWindow(ProviderSession session) {
        Page page = getPage(session);
        int[] size = getScreenSize(page);
        page.setViewportSize(size[0], size[1]);
    }

    private int[] getScreenSize(Page page) {
        Object width = page.evaluate("() => screen.availWidth");
        Object height = page.evaluate("() => screen.availHeight");
        if (width instanceof Number && height instanceof Number) {
            return new int[] { ((Number) width).intValue(), ((Number) height).intValue() };
        }
        return new int[] { 1920, 1080 };
    }

    @Override
    public void minimizeWindow(ProviderSession session) {
        Map<String, Object> bounds = new HashMap<>();
        bounds.put("windowState", "minimized");
        setWindowBounds(session, bounds);
    }

    private void setWindowBounds(ProviderSession session, Map<String, Object> bounds) {
        PlaywrightSessionContext ctx = getCtx(session);
        CDPSession cdp = createCdpSession(ctx);
        if (cdp == null) {
            if (tryMoveWindow(ctx.page, bounds)) {
                return;
            }
            throw new UnsupportedOperationException("Window bounds require a Chromium CDP session.");
        }
        Number windowId = resolveWindowId(cdp, ctx.page);
        if (windowId == null) {
            if (tryMoveWindow(ctx.page, bounds)) {
                return;
            }
            throw new UnsupportedOperationException("Unable to obtain windowId via CDP.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("windowId", windowId);
        params.put("bounds", bounds);
        cdp.send("Browser.setWindowBounds", toJsonObject(params));
    }

    // ==================== Capabilities ====================

    @Override
    public Map<String, Object> getCapabilities(ProviderSession session) {
        Map<String, Object> caps = new HashMap<>(session.getCapabilities().getOptions());
        PlaywrightSessionContext ctx = getCtx(session);
        if (ctx.cdpUrl != null && !caps.containsKey("se:cdp")) {
            caps.put("se:cdp", ctx.cdpUrl);
        }
        return caps;
    }

    private Map<String, JsonElement> resolveWindowBounds(PlaywrightSessionContext ctx) {
        CDPSession cdp = createCdpSession(ctx);
        if (cdp == null) {
            return null;
        }
        Map<String, JsonElement> res = resolveWindowForTarget(cdp, ctx.page);
        if (res == null) {
            return null;
        }
        JsonElement bounds = res.get("bounds");
        if (bounds != null && bounds.isJsonObject()) {
            return bounds.getAsJsonObject().asMap();
        }
        return null;
    }

    private Number resolveWindowId(CDPSession cdp, Page page) {
        Map<String, JsonElement> res = resolveWindowForTarget(cdp, page);
        if (res == null) {
            return null;
        }
        JsonElement windowId = res.get("windowId");
        return windowId != null && windowId.isJsonPrimitive() ? windowId.getAsInt() : null;
    }

    private Map<String, JsonElement> resolveWindowForTarget(CDPSession cdp, Page page) {
        Map<String, JsonElement> res = sendGetWindowForTarget(cdp, new JsonObject());
        if (res != null && res.get("windowId") != null) {
            return res;
        }
        String targetId = resolveTargetId(cdp);
        if (targetId == null) {
            targetId = resolveTargetIdByPage(cdp, page);
        }
        if (targetId != null && !targetId.isEmpty()) {
            JsonObject params = new JsonObject();
            params.addProperty("targetId", targetId);
            res = sendGetWindowForTarget(cdp, params);
        }
        return res;
    }

    private Map<String, JsonElement> sendGetWindowForTarget(CDPSession cdp, JsonObject params) {
        Object res = cdp.send("Browser.getWindowForTarget", params);
        if (res instanceof JsonObject json) {
            return json.asMap();
        }
        return null;
    }

    private CDPSession createCdpSession(PlaywrightSessionContext ctx) {
        try {
            return ctx.context.newCDPSession(ctx.page);
        } catch (PlaywrightException e) {
            return null;
        }
    }

    private JsonObject toJsonObject(Map<String, Object> map) {
        return new Gson().toJsonTree(map).getAsJsonObject();
    }

    private JsonObject buildWindowForTargetParams(CDPSession cdp) {
        JsonObject params = new JsonObject();
        String targetId = resolveTargetId(cdp);
        if (targetId != null && !targetId.isEmpty()) {
            params.addProperty("targetId", targetId);
        }
        return params;
    }

    private String resolveTargetId(CDPSession cdp) {
        try {
            Object res = cdp.send("Target.getTargetInfo", new JsonObject());
            if (!(res instanceof Map)) {
                return null;
            }
            Object info = ((Map) res).get("targetInfo");
            if (info instanceof Map) {
                Object targetId = ((Map) info).get("targetId");
                return targetId == null ? null : targetId.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveTargetIdByPage(CDPSession cdp, Page page) {
        try {
            Object res = cdp.send("Target.getTargets", new JsonObject());
            if (!(res instanceof Map)) {
                return null;
            }
            Object infos = ((Map) res).get("targetInfos");
            if (!(infos instanceof List)) {
                return null;
            }
            String url = page.url();
            String title = page.title();
            for (Object obj : (List) infos) {
                if (!(obj instanceof Map)) {
                    continue;
                }
                Map info = (Map) obj;
                Object type = info.get("type");
                if (type != null && !"page".equals(type.toString())) {
                    continue;
                }
                Object infoUrl = info.get("url");
                Object infoTitle = info.get("title");
                boolean match = (url != null && url.equals(infoUrl))
                        || (title != null && title.equals(infoTitle));
                if (match) {
                    Object targetId = info.get("targetId");
                    return targetId == null ? null : targetId.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean tryMoveWindow(Page page, Map<String, Object> bounds) {
        if (bounds == null) {
            return false;
        }
        Object leftObj = bounds.get("left");
        Object topObj = bounds.get("top");
        if (!(leftObj instanceof Number) || !(topObj instanceof Number)) {
            return false;
        }
        int left = ((Number) leftObj).intValue();
        int top = ((Number) topObj).intValue();
        try {
            Object result = page.evaluate(
                    "([x,y]) => { if (typeof window.moveTo === 'function') { window.moveTo(x,y); return true; } return false; }",
                    Arrays.asList(left, top));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private int toInt(Object value) {
        if (value instanceof JsonElement jsonElement && jsonElement.isJsonPrimitive()
                && jsonElement.getAsJsonPrimitive().isNumber()) {
            return jsonElement.getAsJsonPrimitive().getAsNumber().intValue();
        } else if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    @Override
    public String getWindowHandle(ProviderSession session) {
        PlaywrightSessionContext ctx = getCtx(session);
        return ctx.getHandle(ctx.page);
    }

    @Override
    public Set<String> getWindowHandles(ProviderSession session) {
        PlaywrightSessionContext ctx = getCtx(session);
        // Refresh handles (remove closed pages)
        ctx.handleMap.values().removeIf(Page::isClosed);
        return new HashSet<>(ctx.handleMap.keySet());
    }

    @Override
    public void switchToWindow(ProviderSession session, String nameOrHandle) {
        PlaywrightSessionContext ctx = getCtx(session);
        Page p = ctx.handleMap.get(nameOrHandle);
        if (p != null) {
            ctx.page = p;
            ctx.activeFrame = p.mainFrame();
            p.bringToFront();
        } else {
            // Fallback: search by title
            for (Page page : ctx.context.pages()) {
                if (nameOrHandle.equals(page.title())) {
                    ctx.page = page;
                    ctx.activeFrame = page.mainFrame();
                    page.bringToFront();
                    return;
                }
            }
            throw new com.dod.hub.core.exception.HubException("No window found with handle or title: " + nameOrHandle);
        }
    }

    @Override
    public void switchToNewWindow(ProviderSession session, com.dod.hub.core.provider.HubWindowType typeHint) {
        PlaywrightSessionContext ctx = getCtx(session);
        Page newPage = ctx.context.newPage();
        ctx.page = newPage;
        ctx.activeFrame = newPage.mainFrame();
        newPage.bringToFront();
    }
}
