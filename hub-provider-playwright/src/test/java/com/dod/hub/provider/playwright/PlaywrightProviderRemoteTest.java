package com.dod.hub.provider.playwright;

import com.dod.hub.core.config.HubBrowserType;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaywrightProviderRemoteTest {

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

    private TestablePlaywrightProvider provider;

    // Subclass to inject mock Playwright
    static class TestablePlaywrightProvider extends PlaywrightProvider {
        private final Playwright mockPlaywright;

        public TestablePlaywrightProvider(Playwright mockPlaywright) {
            this.mockPlaywright = mockPlaywright;
        }

        @Override
        protected Playwright createPlaywright() {
            return mockPlaywright;
        }
    }

    @BeforeEach
    void setUp() {
        provider = new TestablePlaywrightProvider(mockPlaywright);

        // Default chain
        when(mockPlaywright.chromium()).thenReturn(mockBrowserType);
        when(mockPlaywright.firefox()).thenReturn(mockBrowserType);
        when(mockPlaywright.webkit()).thenReturn(mockBrowserType);

        when(mockBrowserType.connect(anyString())).thenReturn(mockBrowser);
        when(mockBrowserType.launch(any())).thenReturn(mockBrowser);

        when(mockBrowser.newContext()).thenReturn(mockContext);
        when(mockContext.newPage()).thenReturn(mockPage);
    }

    @Test
    @DisplayName("start() should CALL connect(gridUrl) when gridUrl is present")
    void startShouldConnectRemotelyWhenGridUrlIsPresent() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        caps.setBrowserName(HubBrowserType.CHROME);
        caps.setGridUrl("ws://my-grid:4444");

        // Act
        ProviderSession session = provider.start(caps);

        // Assert
        assertNotNull(session);

        // Verify CONNECT was called with implicit gridUrl
        verify(mockBrowserType).connect(eq("ws://my-grid:4444"));
        verify(mockBrowserType, never()).launch(any());
    }

    @Test
    @DisplayName("start() should CALL launch(options) when gridUrl is missing")
    void startShouldLaunchLocallyWhenGridUrlIsMissing() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        caps.setBrowserName(HubBrowserType.FIREFOX);
        caps.setGridUrl(null);

        // Act
        ProviderSession session = provider.start(caps);

        // Assert
        assertNotNull(session);

        // Verify LAUNCH was called
        verify(mockBrowserType).launch(any(BrowserType.LaunchOptions.class));
        verify(mockBrowserType, never()).connect(anyString());
    }
}
