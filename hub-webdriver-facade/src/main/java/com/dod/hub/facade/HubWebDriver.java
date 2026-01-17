package com.dod.hub.facade;

import com.dod.hub.core.command.CommandType;
import com.dod.hub.core.command.HubCommand;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.pipeline.CommandContext;
import com.dod.hub.core.pipeline.CommandPipeline;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.Logs;

import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HubWebDriver implements WebDriver, TakesScreenshot, JavascriptExecutor {

    private final HubProvider provider;
    private final CommandPipeline pipeline;
    private final SessionCapabilities caps;
    private volatile ProviderSession session;

    private long implicitWaitMs = 0;
    private long pageLoadTimeoutMs = 0;

    public HubWebDriver(HubProvider provider, SessionCapabilities caps) {
        this.provider = provider;
        this.pipeline = new CommandPipeline();
        this.caps = caps;
    }

    public HubWebDriver(HubProvider provider) {
        this(provider, new SessionCapabilities());
    }

    /**
     * Internal constructor for proxy instantiation and framework-level testing.
     * Should not be used for direct initialization.
     */
    protected HubWebDriver() {
        this.provider = null;
        this.pipeline = null;
        this.caps = null;
    }

    /**
     * Lazily initializes and returns the current provider session.
     * 
     * @return The active {@link ProviderSession}.
     */
    public ProviderSession getSession() {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    session = provider.start(caps);
                    provider.setTimeouts(session, implicitWaitMs, pageLoadTimeoutMs);
                }
            }
        }
        return session;
    }

    public HubProvider getProvider() {
        return provider;
    }

    public CommandPipeline getPipeline() {
        return pipeline;
    }

    private CommandContext ctx(CommandType type, String target) {
        ProviderSession s = getSession();
        HubCommand cmd = new HubCommand(type, s.getSessionId(), provider.getName());
        cmd.setTarget(target);
        return new CommandContext(s, provider, cmd);
    }

    /**
     * Navigation to a specified URL.
     *
     * @param url The target URL.
     */
    @Override
    public void get(String url) {
        ProviderSession s = getSession();
        HubCommand cmd = new HubCommand(CommandType.NAVIGATE_TO, s.getSessionId(), provider.getName());
        cmd.addParam("url", url);
        cmd.setTarget(url);

        CommandContext context = new CommandContext(s, provider, cmd);
        pipeline.execute(context, () -> {
            provider.navigate(s, url);
            return null;
        });
    }

    @Override
    public String getCurrentUrl() {
        CommandContext context = ctx(CommandType.GET_CURRENT_URL, HubCommand.TARGET_BROWSER);
        return pipeline.execute(context, () -> provider.getCurrentUrl(getSession()));
    }

    @Override
    public String getTitle() {
        CommandContext context = ctx(CommandType.GET_TITLE, HubCommand.TARGET_BROWSER);
        return pipeline.execute(context, () -> provider.getTitle(getSession()));
    }

    @Override
    public List<WebElement> findElements(By by) {
        HubLocator locator = HubBy.toHubLocator(by);
        CommandContext context = ctx(CommandType.FIND_ELEMENTS, locator.toString());

        return pipeline.execute(context, () -> {
            List<HubElementRef> refs = provider.findAll(getSession(), locator);
            return refs.stream()
                    .map(ref -> new HubWebElement(this, ref))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Finds the first element matching the given locator.
     *
     * @param by The Selenium locator to use.
     * @return The found {@link WebElement} wrapped as a {@link HubWebElement}.
     */
    @Override
    public WebElement findElement(By by) {
        HubLocator locator = HubBy.toHubLocator(by);
        CommandContext context = ctx(CommandType.FIND_ELEMENT, locator.toString());

        return pipeline.execute(context, () -> {
            HubElementRef ref = provider.find(getSession(), locator);
            return new HubWebElement(this, ref);
        });
    }

    @Override
    public String getPageSource() {
        CommandContext context = ctx(CommandType.PAGE_SOURCE, HubCommand.TARGET_BROWSER);
        return pipeline.execute(context, () -> provider.getPageSource(getSession()));
    }

    @Override
    public void close() {
        quit();
    }

    @Override
    public void quit() {
        if (session == null) {
            return;
        }
        CommandContext context = ctx(CommandType.SESSION_END, HubCommand.TARGET_BROWSER);
        pipeline.execute(context, () -> {
            provider.stop(session);
            return null;
        });
        session = null;
    }

    @Override
    public Set<String> getWindowHandles() {
        return Set.of(getSession().getSessionId());
    }

    @Override
    public String getWindowHandle() {
        return getSession().getSessionId();
    }

    @Override
    public TargetLocator switchTo() {
        throw new UnsupportedOperationException("Window/Frame switching is not supported in the current version.");
    }

    @Override
    public Navigation navigate() {
        return new Navigation() {
            @Override
            public void back() {
                CommandContext context = ctx(CommandType.NAV_BACK, HubCommand.TARGET_BROWSER);
                pipeline.execute(context, () -> {
                    provider.back(getSession());
                    return null;
                });
            }

            @Override
            public void forward() {
                CommandContext context = ctx(CommandType.NAV_FORWARD, HubCommand.TARGET_BROWSER);
                pipeline.execute(context, () -> {
                    provider.forward(getSession());
                    return null;
                });
            }

            @Override
            public void to(String url) {
                get(url);
            }

            @Override
            public void to(URL url) {
                get(url.toString());
            }

            @Override
            public void refresh() {
                CommandContext context = ctx(CommandType.NAV_REFRESH, HubCommand.TARGET_BROWSER);
                pipeline.execute(context, () -> {
                    provider.refresh(getSession());
                    return null;
                });
            }
        };
    }

    @Override
    public Options manage() {
        return new Options() {
            @Override
            public void addCookie(Cookie cookie) {
                provider.addCookie(getSession(), cookie.getName(), cookie.getValue(),
                        cookie.getDomain(), cookie.getPath());
            }

            @Override
            public void deleteCookieNamed(String name) {
                provider.deleteCookie(getSession(), name);
            }

            @Override
            public void deleteCookie(Cookie cookie) {
                provider.deleteCookie(getSession(), cookie.getName());
            }

            @Override
            public void deleteAllCookies() {
                provider.deleteAllCookies(getSession());
            }

            @Override
            public Set<Cookie> getCookies() {
                Set<Map<String, Object>> providerCookies = provider.getCookies(getSession());
                Set<Cookie> result = new HashSet<>();
                for (Map<String, Object> map : providerCookies) {
                    Cookie.Builder builder = new Cookie.Builder(
                            (String) map.get("name"),
                            (String) map.get("value"));
                    if (map.get("domain") != null)
                        builder.domain((String) map.get("domain"));
                    if (map.get("path") != null)
                        builder.path((String) map.get("path"));
                    if (map.get("expiry") != null && map.get("expiry") instanceof Date) {
                        builder.expiresOn((Date) map.get("expiry"));
                    }
                    if (map.get("secure") != null)
                        builder.isSecure((Boolean) map.get("secure"));
                    if (map.get("httpOnly") != null)
                        builder.isHttpOnly((Boolean) map.get("httpOnly"));
                    result.add(builder.build());
                }
                return result;
            }

            @Override
            public Cookie getCookieNamed(String name) {
                Map<String, Object> map = provider.getCookie(getSession(), name);
                if (map == null)
                    return null;
                Cookie.Builder builder = new Cookie.Builder(
                        (String) map.get("name"),
                        (String) map.get("value"));
                if (map.get("domain") != null)
                    builder.domain((String) map.get("domain"));
                if (map.get("path") != null)
                    builder.path((String) map.get("path"));
                if (map.get("expiry") != null && map.get("expiry") instanceof Date) {
                    builder.expiresOn((Date) map.get("expiry"));
                }
                if (map.get("secure") != null)
                    builder.isSecure((Boolean) map.get("secure"));
                if (map.get("httpOnly") != null)
                    builder.isHttpOnly((Boolean) map.get("httpOnly"));
                return builder.build();
            }

            @Override
            public Timeouts timeouts() {
                return new Timeouts() {
                    @Override
                    public Timeouts implicitlyWait(Duration duration) {
                        implicitWaitMs = duration.toMillis();
                        if (session != null) {
                            provider.setTimeouts(session, implicitWaitMs, pageLoadTimeoutMs);
                        }
                        return this;
                    }

                    @Override
                    public Timeouts implicitlyWait(long time, TimeUnit unit) {
                        return implicitlyWait(Duration.ofMillis(unit.toMillis(time)));
                    }

                    @Override
                    public Timeouts pageLoadTimeout(Duration duration) {
                        pageLoadTimeoutMs = duration.toMillis();
                        if (session != null) {
                            provider.setTimeouts(session, implicitWaitMs, pageLoadTimeoutMs);
                        }
                        return this;
                    }

                    @Override
                    public Timeouts pageLoadTimeout(long time, TimeUnit unit) {
                        return pageLoadTimeout(Duration.ofMillis(unit.toMillis(time)));
                    }

                    @Override
                    public Timeouts scriptTimeout(Duration duration) {
                        return this;
                    }

                    @Override
                    public Timeouts setScriptTimeout(long time, TimeUnit unit) {
                        return this;
                    }
                };
            }

            @Override
            public Window window() {
                return new Window() {
                    @Override
                    public Dimension getSize() {
                        int[] size = provider.getWindowSize(getSession());
                        if (size == null)
                            return new Dimension(0, 0);
                        return new Dimension(size[0], size[1]);
                    }

                    @Override
                    public void setSize(Dimension targetSize) {
                        provider.setWindowSize(getSession(), targetSize.getWidth(), targetSize.getHeight());
                    }

                    @Override
                    public Point getPosition() {
                        int[] pos = provider.getWindowPosition(getSession());
                        if (pos == null)
                            return new Point(0, 0);
                        return new Point(pos[0], pos[1]);
                    }

                    @Override
                    public void setPosition(Point targetPosition) {
                        provider.setWindowPosition(getSession(), targetPosition.getX(), targetPosition.getY());
                    }

                    @Override
                    public void maximize() {
                        provider.maximizeWindow(getSession());
                    }

                    @Override
                    public void minimize() {
                        provider.minimizeWindow(getSession());
                    }

                    @Override
                    public void fullscreen() {
                        provider.fullscreenWindow(getSession());
                    }
                };
            }

            @Override
            public Logs logs() {
                return new Logs() {
                    @Override
                    public LogEntries get(String logType) {
                        return new LogEntries(Collections.emptyList());
                    }

                    @Override
                    public Set<String> getAvailableLogTypes() {
                        return Collections.emptySet();
                    }
                };
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        CommandContext context = ctx(CommandType.SCREENSHOT, HubCommand.TARGET_BROWSER);
        byte[] bytes = pipeline.execute(context, () -> provider.takeScreenshot(getSession()));

        if (target == OutputType.BYTES)
            return (X) bytes;
        if (target == OutputType.BASE64)
            return (X) Base64.getEncoder().encodeToString(bytes);
        throw new UnsupportedOperationException("Only BYTES and BASE64 output types are supported.");
    }

    @Override
    public Object executeScript(String script, Object... args) {
        CommandContext context = ctx(CommandType.EXECUTE_SCRIPT, HubCommand.TARGET_BROWSER);
        return pipeline.execute(context, () -> provider.executeScript(getSession(), script, args));
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        CommandContext context = ctx(CommandType.EXECUTE_ASYNC_SCRIPT, HubCommand.TARGET_BROWSER);
        return pipeline.execute(context, () -> provider.executeAsyncScript(getSession(), script, args));
    }
}
