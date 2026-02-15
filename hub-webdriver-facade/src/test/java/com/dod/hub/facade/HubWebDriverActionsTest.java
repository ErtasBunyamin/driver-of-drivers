package com.dod.hub.facade;

import com.dod.hub.core.geometry.HubRect;
import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HubWebDriverActionsTest {

    MockActionsProvider mockProvider;
    HubWebDriver driver;

    @BeforeEach
    void setUp() {
        mockProvider = new MockActionsProvider();
        driver = new HubWebDriver(mockProvider, new SessionCapabilities());
    }

    @Test
    @DisplayName("HubWebDriver should implement Interactive interface")
    void implementsInteractive() {
        assertTrue(driver instanceof Interactive, "HubWebDriver must implement Interactive");
    }

    @Test
    @DisplayName("perform(actions) should delegate to provider.performActions")
    void performDelegatesToProvider() {
        Interactive interactiveDriver = (Interactive) driver;
        Collection<Sequence> sequences = Collections.emptyList(); // Empty list is fine for testing delegation

        interactiveDriver.perform(sequences);

        assertTrue(mockProvider.performActionsCalled, "Provider.performActions should be called");
        assertEquals(sequences, mockProvider.lastActions, "Should pass the exact actions collection");
    }

    @Test
    @DisplayName("resetInputState() should delegate to provider.resetInputState")
    void resetInputStateDelegatesToProvider() {
        Interactive interactiveDriver = (Interactive) driver;

        interactiveDriver.resetInputState();

        assertTrue(mockProvider.resetInputStateCalled, "Provider.resetInputState should be called");
    }

    @Test
    @DisplayName("getRect/getLocation/getSize should delegate to provider.getRect")
    void geometryDelegatesToProvider() {
        WebElement element = driver.findElement(By.id("foo"));

        Rectangle rect = element.getRect();
        assertEquals(10, rect.x);
        assertEquals(20, rect.y);
        assertEquals(100, rect.width);
        assertEquals(200, rect.height);

        Point loc = element.getLocation();
        assertEquals(10, loc.x);
        assertEquals(20, loc.y);

        Dimension size = element.getSize();
        assertEquals(100, size.width);
        assertEquals(200, size.height);
    }

    // Minimal Mock Provider
    static class MockActionsProvider implements HubProvider {

        boolean performActionsCalled = false;
        Collection<?> lastActions;

        boolean resetInputStateCalled = false;

        @Override
        public String getName() {
            return "mock-actions";
        }

        @Override
        public ProviderSession start(SessionCapabilities caps) {
            return new ProviderSession("mock", caps, new Object());
        }

        @Override
        public void performActions(ProviderSession session, Collection<?> actions) {
            performActionsCalled = true;
            lastActions = actions;
        }

        @Override
        public void resetInputState(ProviderSession session) {
            resetInputStateCalled = true;
        }

        // --- Required Stub Implementations ---
        @Override
        public void stop(ProviderSession session) {
        }

        @Override
        public HubElementRef find(ProviderSession session, HubLocator locator) {
            // Return a dummy ref so HubWebElement can be created
            return new HubElementRef(locator, new Object());
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
        public HubRect getRect(ProviderSession session, HubElementRef element) {
            return new HubRect(10, 20, 100, 200);
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
        }

        @Override
        public void maximizeWindow(ProviderSession session) {
        }

        @Override
        public void minimizeWindow(ProviderSession session) {
        }

        @Override
        public void fullscreenWindow(ProviderSession session) {
        }

        @Override
        public int[] getWindowSize(ProviderSession session) {
            return new int[] { 0, 0 };
        }

        @Override
        public void setWindowSize(ProviderSession session, int width, int height) {
        }

        @Override
        public int[] getWindowPosition(ProviderSession session) {
            return new int[] { 0, 0 };
        }

        @Override
        public void setWindowPosition(ProviderSession session, int x, int y) {
        }

        @Override
        public void addCookie(ProviderSession session, String name, String value, String domain, String path) {
        }

        @Override
        public void deleteCookie(ProviderSession session, String name) {
        }

        @Override
        public void deleteAllCookies(ProviderSession session) {
        }

        @Override
        public Set<Map<String, Object>> getCookies(ProviderSession session) {
            return Collections.emptySet();
        }

        @Override
        public void switchToWindow(ProviderSession session, String nameOrHandle) {
        }

        @Override
        public void switchToNewWindow(ProviderSession session, com.dod.hub.core.provider.HubWindowType typeHint) {
        }

        @Override
        public String getWindowHandle(ProviderSession session) {
            return "";
        }

        @Override
        public Set<String> getWindowHandles(ProviderSession session) {
            return Collections.emptySet();
        }

        @Override
        public void acceptAlert(ProviderSession session) {
        }

        @Override
        public void dismissAlert(ProviderSession session) {
        }

        @Override
        public String getAlertText(ProviderSession session) {
            return "";
        }

        @Override
        public void sendKeysToAlert(ProviderSession session, String text) {
        }

        @Override
        public void switchToFrame(ProviderSession session, int index) {
        }

        @Override
        public void switchToFrame(ProviderSession session, String nameOrId) {
        }

        @Override
        public void switchToFrame(ProviderSession session, HubElementRef frameElement) {
        }

        @Override
        public void switchToParentFrame(ProviderSession session) {
        }

        @Override
        public void switchToDefaultContent(ProviderSession session) {
        }

        @Override
        public Object executeScript(ProviderSession session, String script, Object... args) {
            return null;
        }

        @Override
        public Object executeAsyncScript(ProviderSession session, String script, Object... args) {
            return null;
        }
    }
}
