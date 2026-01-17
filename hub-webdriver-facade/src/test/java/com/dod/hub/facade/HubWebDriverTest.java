package com.dod.hub.facade;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HubWebDriver to ensure:
 * 1. No NullPointerExceptions from manage().window() and manage().logs()
 * 2. Cookie management delegates to provider
 * 3. Window management delegates to provider
 * 4. JavaScript execution delegates to provider
 */
class HubWebDriverTest {

    MockProvider mockProvider;
    HubWebDriver driver;

    @BeforeEach
    void setUp() {
        mockProvider = new MockProvider();
        driver = new HubWebDriver(mockProvider, new SessionCapabilities());
    }

    @Nested
    @DisplayName("Window Management Tests")
    class WindowManagementTests {

        @Test
        @DisplayName("manage().window() should NOT return null")
        void windowShouldNotReturnNull() {
            WebDriver.Window window = driver.manage().window();
            assertNotNull(window, "manage().window() should never return null to prevent NPE");
        }

        @Test
        @DisplayName("window().getSize() should delegate to provider")
        void getWindowSizeDelegatesToProvider() {
            mockProvider.windowSize = new int[] { 1024, 768 };

            Dimension size = driver.manage().window().getSize();

            assertNotNull(size);
            assertEquals(1024, size.getWidth());
            assertEquals(768, size.getHeight());
            assertTrue(mockProvider.getWindowSizeCalled);
        }

        @Test
        @DisplayName("window().setSize() should delegate to provider")
        void setWindowSizeDelegatesToProvider() {
            driver.manage().window().setSize(new Dimension(1920, 1080));

            assertTrue(mockProvider.setWindowSizeCalled);
            assertEquals(1920, mockProvider.lastSetWidth);
            assertEquals(1080, mockProvider.lastSetHeight);
        }

        @Test
        @DisplayName("window().getPosition() should delegate to provider")
        void getWindowPositionDelegatesToProvider() {
            mockProvider.windowPosition = new int[] { 100, 200 };

            Point pos = driver.manage().window().getPosition();

            assertNotNull(pos);
            assertEquals(100, pos.getX());
            assertEquals(200, pos.getY());
            assertTrue(mockProvider.getWindowPositionCalled);
        }

        @Test
        @DisplayName("window().maximize() should delegate to provider")
        void maximizeDelegatesToProvider() {
            driver.manage().window().maximize();
            assertTrue(mockProvider.maximizeWindowCalled);
        }

        @Test
        @DisplayName("window().minimize() should delegate to provider")
        void minimizeDelegatesToProvider() {
            driver.manage().window().minimize();
            assertTrue(mockProvider.minimizeWindowCalled);
        }

        @Test
        @DisplayName("window().fullscreen() should delegate to provider")
        void fullscreenDelegatesToProvider() {
            driver.manage().window().fullscreen();
            assertTrue(mockProvider.fullscreenWindowCalled);
        }
    }

    @Nested
    @DisplayName("Logs Management Tests")
    class LogsManagementTests {

        @Test
        @DisplayName("manage().logs() should NOT return null")
        void logsShouldNotReturnNull() {
            org.openqa.selenium.logging.Logs logs = driver.manage().logs();
            assertNotNull(logs, "manage().logs() should never return null to prevent NPE");
        }

        @Test
        @DisplayName("logs().get() should return empty entries instead of NPE")
        void logsGetReturnsEmptyEntries() {
            var entries = driver.manage().logs().get("browser");
            assertNotNull(entries);
            assertFalse(entries.iterator().hasNext());
        }

        @Test
        @DisplayName("logs().getAvailableLogTypes() should return empty set")
        void logsGetAvailableLogTypesReturnsEmptySet() {
            var types = driver.manage().logs().getAvailableLogTypes();
            assertNotNull(types);
            assertTrue(types.isEmpty());
        }
    }

    @Nested
    @DisplayName("Cookie Management Tests")
    class CookieManagementTests {

        @Test
        @DisplayName("addCookie should delegate to provider")
        void addCookieDelegatesToProvider() {
            Cookie cookie = new Cookie("test", "value", "example.com", "/", null);

            driver.manage().addCookie(cookie);

            assertTrue(mockProvider.addCookieCalled);
            assertEquals("test", mockProvider.lastAddedCookieName);
            assertEquals("value", mockProvider.lastAddedCookieValue);
        }

        @Test
        @DisplayName("deleteCookieNamed should delegate to provider")
        void deleteCookieNamedDelegatesToProvider() {
            driver.manage().deleteCookieNamed("test");

            assertTrue(mockProvider.deleteCookieCalled);
            assertEquals("test", mockProvider.lastDeletedCookieName);
        }

        @Test
        @DisplayName("deleteAllCookies should delegate to provider")
        void deleteAllCookiesDelegatesToProvider() {
            driver.manage().deleteAllCookies();

            assertTrue(mockProvider.deleteAllCookiesCalled);
        }

        @Test
        @DisplayName("getCookies should delegate to provider and convert properly")
        void getCookiesDelegatesToProvider() {
            Map<String, Object> cookieMap = new HashMap<>();
            cookieMap.put("name", "testCookie");
            cookieMap.put("value", "testValue");
            mockProvider.cookies.add(cookieMap);

            Set<Cookie> cookies = driver.manage().getCookies();

            assertNotNull(cookies);
            assertEquals(1, cookies.size());
            Cookie cookie = cookies.iterator().next();
            assertEquals("testCookie", cookie.getName());
            assertEquals("testValue", cookie.getValue());
            assertTrue(mockProvider.getCookiesCalled);
        }
    }

    @Nested
    @DisplayName("JavaScript Execution Tests")
    class JavaScriptExecutionTests {

        @Test
        @DisplayName("executeScript should delegate to provider")
        void executeScriptDelegatesToProvider() {
            mockProvider.scriptResult = "result";

            Object result = driver.executeScript("return 'test';");

            assertEquals("result", result);
            assertTrue(mockProvider.executeScriptCalled);
            assertEquals("return 'test';", mockProvider.lastScript);
        }

        @Test
        @DisplayName("executeScript with arguments should delegate correctly")
        void executeScriptWithArgsDelegatesToProvider() {
            mockProvider.scriptResult = 42;

            Object result = driver.executeScript("return arguments[0] + arguments[1];", 1, 2);

            assertEquals(42, result);
            assertEquals(2, mockProvider.lastScriptArgs.length);
            assertEquals(1, mockProvider.lastScriptArgs[0]);
        }

        @Test
        @DisplayName("executeAsyncScript should delegate to provider")
        void executeAsyncScriptDelegatesToProvider() {
            mockProvider.asyncScriptResult = "async-result";

            Object result = driver.executeAsyncScript("callback('done');");

            assertEquals("async-result", result);
            assertTrue(mockProvider.executeAsyncScriptCalled);
        }
    }

    @Nested
    @DisplayName("Timeout Tests")
    class TimeoutTests {

        @Test
        @DisplayName("implicitlyWait should update internal state and delegate to provider when session exists")
        void implicitlyWaitDelegatesToProvider() {
            // First call getSession to ensure session is created
            driver.getSession();

            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));

            assertEquals(10000L, mockProvider.lastImplicitWait);
        }

        @Test
        @DisplayName("pageLoadTimeout should update internal state")
        void pageLoadTimeoutDelegatesToProvider() {
            driver.getSession();

            driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(30));

            assertEquals(30000L, mockProvider.lastPageLoadTimeout);
        }
    }

    // Concrete Mock Implementation
    static class MockProvider implements HubProvider {

        boolean maximizeWindowCalled = false;
        boolean minimizeWindowCalled = false;
        boolean fullscreenWindowCalled = false;
        boolean getWindowSizeCalled = false;
        boolean setWindowSizeCalled = false;
        boolean getWindowPositionCalled = false;

        boolean addCookieCalled = false;
        boolean deleteCookieCalled = false;
        boolean deleteAllCookiesCalled = false;
        boolean getCookiesCalled = false;

        boolean executeScriptCalled = false;
        boolean executeAsyncScriptCalled = false;

        int[] windowSize = new int[] { 0, 0 };
        int[] windowPosition = new int[] { 0, 0 };
        int lastSetWidth, lastSetHeight;

        String lastAddedCookieName, lastAddedCookieValue;
        String lastDeletedCookieName;
        Set<Map<String, Object>> cookies = new HashSet<>();

        String lastScript;
        Object[] lastScriptArgs;
        Object scriptResult;
        Object asyncScriptResult;

        long lastImplicitWait;
        long lastPageLoadTimeout;

        @Override
        public String getName() {
            return "mock";
        }

        @Override
        public ProviderSession start(SessionCapabilities caps) {
            return new ProviderSession("mock", caps, new Object());
        }

        @Override
        public void stop(ProviderSession session) {
        }

        @Override
        public HubElementRef find(ProviderSession session, HubLocator locator) {
            return null;
        }

        @Override
        public List<HubElementRef> findAll(ProviderSession session, HubLocator locator) {
            return Collections.emptyList();
        }

        @Override
        public HubElementRef find(ProviderSession session, HubElementRef parent, HubLocator locator) {
            return null;
        }

        @Override
        public List<HubElementRef> findAll(ProviderSession session, HubElementRef parent, HubLocator locator) {
            return Collections.emptyList();
        }

        @Override
        public void click(ProviderSession session, HubElementRef element) {
        }

        @Override
        public void type(ProviderSession session, HubElementRef element, String text) {
        }

        @Override
        public void clear(ProviderSession session, HubElementRef element) {
        }

        @Override
        public String getText(ProviderSession session, HubElementRef element) {
            return "";
        }

        @Override
        public String getAttribute(ProviderSession session, HubElementRef element, String attributeName) {
            return "";
        }

        @Override
        public boolean isDisplayed(ProviderSession session, HubElementRef element) {
            return false;
        }

        @Override
        public boolean isEnabled(ProviderSession session, HubElementRef element) {
            return false;
        }

        @Override
        public boolean isSelected(ProviderSession session, HubElementRef element) {
            return false;
        }

        @Override
        public void navigate(ProviderSession session, String url) {
        }

        @Override
        public void back(ProviderSession session) {
        }

        @Override
        public void forward(ProviderSession session) {
        }

        @Override
        public void refresh(ProviderSession session) {
        }

        @Override
        public String getTitle(ProviderSession session) {
            return "";
        }

        @Override
        public String getCurrentUrl(ProviderSession session) {
            return "";
        }

        @Override
        public String getPageSource(ProviderSession session) {
            return "";
        }

        @Override
        public byte[] takeScreenshot(ProviderSession session) {
            return new byte[0];
        }

        @Override
        public void setTimeouts(ProviderSession session, long implicitWaitMs, long pageLoadMs) {
            this.lastImplicitWait = implicitWaitMs;
            this.lastPageLoadTimeout = pageLoadMs;
        }

        // --- New Interface Methods ---

        @Override
        public void maximizeWindow(ProviderSession session) {
            maximizeWindowCalled = true;
        }

        @Override
        public void minimizeWindow(ProviderSession session) {
            minimizeWindowCalled = true;
        }

        @Override
        public void fullscreenWindow(ProviderSession session) {
            fullscreenWindowCalled = true;
        }

        @Override
        public int[] getWindowSize(ProviderSession session) {
            getWindowSizeCalled = true;
            return windowSize;
        }

        @Override
        public void setWindowSize(ProviderSession session, int width, int height) {
            setWindowSizeCalled = true;
            lastSetWidth = width;
            lastSetHeight = height;
        }

        @Override
        public int[] getWindowPosition(ProviderSession session) {
            getWindowPositionCalled = true;
            return windowPosition;
        }

        @Override
        public void addCookie(ProviderSession session, String name, String value, String domain, String path) {
            addCookieCalled = true;
            lastAddedCookieName = name;
            lastAddedCookieValue = value;
        }

        @Override
        public void deleteCookie(ProviderSession session, String name) {
            deleteCookieCalled = true;
            lastDeletedCookieName = name;
        }

        @Override
        public void deleteAllCookies(ProviderSession session) {
            deleteAllCookiesCalled = true;
        }

        @Override
        public Set<Map<String, Object>> getCookies(ProviderSession session) {
            getCookiesCalled = true;
            return cookies;
        }

        @Override
        public Object executeScript(ProviderSession session, String script, Object... args) {
            executeScriptCalled = true;
            lastScript = script;
            lastScriptArgs = args;
            return scriptResult;
        }

        @Override
        public Object executeAsyncScript(ProviderSession session, String script, Object... args) {
            executeAsyncScriptCalled = true;
            return asyncScriptResult;
        }
    }
}
