package com.dod.hub.provider.playwright;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.locator.LocatorStrategy;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaywrightProviderTest {

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
    @Mock
    private Frame mockFrame;
    @Mock
    private Locator mockLocator;
    @Mock
    private ElementHandle mockElementHandle;

    private TestablePlaywrightProvider provider;
    private ProviderSession session;

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

        when(mockPlaywright.chromium()).thenReturn(mockBrowserType);
        when(mockBrowserType.launch(any())).thenReturn(mockBrowser);
        when(mockBrowser.newContext()).thenReturn(mockContext);
        when(mockContext.newPage()).thenReturn(mockPage);
        when(mockPage.mainFrame()).thenReturn(mockFrame);

        session = provider.start(new SessionCapabilities());
    }

    @Test
    @DisplayName("executeScript should handle Selenium-style arguments[0]")
    void executeScriptShouldHandleArguments0() {
        // Arrange
        String script = "return arguments[0];";
        String arg = "hello";

        // Act
        provider.executeScript(session, script, arg);

        // Assert: Verify that the script passed to Playwright is a wrapper function
        verify(mockFrame).evaluate(contains("const arguments = args;"), anyList());
    }

    @Test
    @DisplayName("executeScript with HubElementRef should pass ElementHandle")
    void executeScriptWithHubElementRef() {
        // Arrange
        HubLocator locator = new HubLocator(LocatorStrategy.ID, "foo");
        HubElementRef ref = new HubElementRef(locator, mockLocator);
        when(mockLocator.elementHandle()).thenReturn(mockElementHandle);

        // Act
        provider.executeScript(session, "return arguments[0]", ref);

        // Assert
        // Verify that evaluate was called with ElementHandle wrapped in a list
        verify(mockFrame).evaluate(contains("const arguments = args;"),
                eq(Collections.singletonList(mockElementHandle)));
    }
}
