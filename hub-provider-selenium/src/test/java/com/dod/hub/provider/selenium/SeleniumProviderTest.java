package com.dod.hub.provider.selenium;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeleniumProviderTest {

    @Mock
    private WebDriver mockDriver;
    @Mock
    private JavascriptExecutor mockJsExecutor;
    @Mock
    private WebElement mockWebElement;

    private TestableSeleniumProvider provider;
    private ProviderSession session;

    // Subclass to inject mock Driver
    static class TestableSeleniumProvider extends SeleniumProvider {
        private final WebDriver mockDriver;

        public TestableSeleniumProvider(WebDriver mockDriver) {
            this.mockDriver = mockDriver;
        }

        @Override
        protected WebDriver createDriver(SessionCapabilities caps, org.openqa.selenium.MutableCapabilities options) {
            return mockDriver;
        }
    }

    @BeforeEach
    void setUp() {
        // Create a mock that implements both WebDriver and JavascriptExecutor
        mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        mockJsExecutor = (JavascriptExecutor) mockDriver;

        provider = new TestableSeleniumProvider(mockDriver);
        session = provider.start(new SessionCapabilities());
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
        // Since executeScript takes Object... varargs, passing an array is expanded.
        // So we verify that it was called with the unwrapped element.
        verify(mockJsExecutor).executeScript(eq(script), eq(mockWebElement));
    }
}
