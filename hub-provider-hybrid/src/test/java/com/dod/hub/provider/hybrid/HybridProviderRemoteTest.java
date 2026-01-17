package com.dod.hub.provider.hybrid;

import com.dod.hub.core.exception.HubException;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HybridProviderRemoteTest {

    @Mock
    private RemoteWebDriver mockSeleniumDriver;
    @Mock
    private Capabilities mockCapabilities;
    @Mock
    private Playwright mockPlaywright;
    @Mock
    private BrowserType mockBrowserType;
    @Mock
    private Browser mockBrowser;
    @Mock
    private BrowserContext mockContext;
    @Mock
    private Page mockPage;

    private TestableHybridProvider provider;

    // Subclass to override factory methods for testing
    static class TestableHybridProvider extends HybridProvider {
        private final WebDriver seleniumDriver;
        private final Playwright playwright;
        private final Browser browser;

        // Flags to verify calls
        boolean createRemoteWebDriverCalled = false;
        boolean connectPlaywrightCalled = false;
        String capturedGridUrl;
        String capturedCdpUrl;

        public TestableHybridProvider(WebDriver seleniumDriver, Playwright playwright, Browser browser) {
            this.seleniumDriver = seleniumDriver;
            this.playwright = playwright;
            this.browser = browser;
        }

        @Override
        protected WebDriver createRemoteWebDriver(String gridUrl, SessionCapabilities caps)
                throws MalformedURLException {
            this.createRemoteWebDriverCalled = true;
            this.capturedGridUrl = gridUrl;
            return seleniumDriver;
        }

        @Override
        protected Playwright createPlaywright() {
            return playwright;
        }

        @Override
        protected Browser connectPlaywright(Playwright playwright, String cdpUrl) {
            this.connectPlaywrightCalled = true;
            this.capturedCdpUrl = cdpUrl;
            return browser;
        }

        // Disable local start for this test suite to avoid side effects
        @Override
        public int[] getWindowSize(ProviderSession session) {
            return new int[] { 1024, 768 };
        }
    }

    @BeforeEach
    void setUp() {
        provider = new TestableHybridProvider(mockSeleniumDriver, mockPlaywright, mockBrowser);

        // Helper mocks for Playwright chain
        when(mockPlaywright.chromium()).thenReturn(mockBrowserType);
        when(mockBrowser.contexts()).thenReturn(Collections.singletonList(mockContext));
        when(mockContext.pages()).thenReturn(Collections.singletonList(mockPage));
    }

    @Test
    @DisplayName("start() should call startRemote when gridUrl is present")
    void startShouldCallStartRemoteWhenGridUrlIsPresent() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        caps.setGridUrl("http://localhost:4444");

        // Mock Selenium capabilities having "se:cdp"
        when(mockSeleniumDriver.getCapabilities()).thenReturn(mockCapabilities);
        when(mockCapabilities.getCapability("se:cdp")).thenReturn("ws://localhost:9222/devtools/browser/123");

        // Act
        ProviderSession session = provider.start(caps);

        // Assert
        assertTrue(provider.createRemoteWebDriverCalled, "Should have called createRemoteWebDriver");
        assertEquals("http://localhost:4444", provider.capturedGridUrl);

        assertTrue(provider.connectPlaywrightCalled, "Should have connected Playwright");
        assertEquals("ws://localhost:9222/devtools/browser/123", provider.capturedCdpUrl);

        assertNotNull(session);
        HybridSession hybridSession = (HybridSession) session;
        assertEquals(mockSeleniumDriver, hybridSession.getSeleniumDriver());
        assertEquals(mockPage, hybridSession.getPlaywrightPage());
    }

    @Test
    @DisplayName("start() should throw exception if se:cdp capability is missing")
    void startShouldThrowIfSeCdpMissing() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        caps.setGridUrl("http://localhost:4444");

        // Mock Selenium capabilities MISSING "se:cdp"
        when(mockSeleniumDriver.getCapabilities()).thenReturn(mockCapabilities);
        when(mockCapabilities.getCapability("se:cdp")).thenReturn(null);

        // Act & Assert
        HubException ex = assertThrows(HubException.class, () -> provider.start(caps));
        assertTrue(ex.getMessage().contains("Could not retrieve CDP endpoint"),
                "Exception should mention missing CDP endpoint");

        // Verify we tried to start selenium but aborted
        assertTrue(provider.createRemoteWebDriverCalled);
        assertFalse(provider.connectPlaywrightCalled, "Should not attempt to connect Playwright if CDP URL is missing");
        verify(mockSeleniumDriver).quit(); // Should clean up
    }

    @Test
    @DisplayName("start() should sanitize CDP URL by replacing host with Grid host")
    void startShouldSanitizeCdpUrlWhenHostsDiffer() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        String publicGridHost = "my-public-grid.com";
        String gridUrl = "http://" + publicGridHost + ":4444";
        caps.setGridUrl(gridUrl);

        // Mock Selenium returning an internal IP (e.g. Docker container IP)
        String internalCdpUrl = "ws://172.18.0.5:5555/devtools/browser/abc-123";
        // Expected sanitization: replace 172.18.0.5 with my-public-grid.com
        String expectedCdpUrl = "ws://" + publicGridHost + ":5555/devtools/browser/abc-123";

        when(mockSeleniumDriver.getCapabilities()).thenReturn(mockCapabilities);
        when(mockCapabilities.getCapability("se:cdp")).thenReturn(internalCdpUrl);

        // Act
        provider.start(caps);

        // Assert
        assertTrue(provider.connectPlaywrightCalled);
        assertEquals(expectedCdpUrl, provider.capturedCdpUrl, "Should have replaced internal Host with Grid Host");
    }
}
