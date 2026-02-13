package com.dod.hub.provider.hybrid;

import com.dod.hub.core.locator.HubElementRef;
import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.locator.LocatorStrategy;
import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HybridProviderTest {

    @Mock
    private WebDriver mockDriver;
    @Mock
    private JavascriptExecutor mockJsExecutor;
    @Mock
    private WebElement mockWebElement;
    @Mock
    private com.microsoft.playwright.Playwright mockPlaywright;
    @Mock
    private com.microsoft.playwright.Browser mockBrowser;
    @Mock
    private com.microsoft.playwright.BrowserContext mockContext;
    @Mock
    private com.microsoft.playwright.Page mockPage;

    private HybridProvider provider;
    private ProviderSession session;

    @BeforeEach
    void setUp() {
        // Mock HybridSession directly to avoid complex start logic
        mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        mockJsExecutor = (JavascriptExecutor) mockDriver;

        session = new HybridSession(
                "hybrid",
                new SessionCapabilities(),
                null,
                mockDriver,
                mockPlaywright,
                mockBrowser,
                mockPage,
                null,
                "ws://localhost:9222");

        provider = new HybridProvider();
    }

    @Test
    @DisplayName("executeScript should unwrap HubElementRef to WebElement")
    void executeScriptShouldUnwrapHubElementRef() {
        // Arrange
        HubLocator locator = new HubLocator(LocatorStrategy.ID, "foo");
        HubElementRef ref = new HubElementRef(locator, mockWebElement);
        String script = "arguments[0].click();";

        // Act
        provider.executeScript(session, script, ref);

        // Assert
        // Verify that executeScript was called with WebElement, NOT HubElementRef
        verify(mockJsExecutor).executeScript(eq(script), eq(mockWebElement));
    }
}
