package com.dod.hub.provider.selenium;

import com.dod.hub.core.provider.ProviderSession;
import com.dod.hub.core.provider.SessionCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class SeleniumProviderRemoteTest {

    @Mock
    private RemoteWebDriver mockRemoteDriver;

    @Mock
    private WebDriver mockLocalDriver;

    private TestableSeleniumProvider provider;

    // Subclass to override factory methods for testing
    static class TestableSeleniumProvider extends SeleniumProvider {
        private final WebDriver remoteDriverToReturn;
        private final WebDriver localDriverToReturn;

        boolean createDriverCalled = false;
        boolean wasRemote = false;

        public TestableSeleniumProvider(WebDriver remoteDriverToReturn, WebDriver localDriverToReturn) {
            this.remoteDriverToReturn = remoteDriverToReturn;
            this.localDriverToReturn = localDriverToReturn;
        }

        @Override
        protected WebDriver createDriver(SessionCapabilities caps, MutableCapabilities options) {
            this.createDriverCalled = true;
            if (caps.getGridUrl() != null && !caps.getGridUrl().isEmpty()) {
                this.wasRemote = true;
                return remoteDriverToReturn;
            } else {
                this.wasRemote = false;
                return localDriverToReturn;
            }
        }

        @Override
        public int[] getWindowSize(ProviderSession session) {
            return new int[] { 1024, 768 };
        }
    }

    @BeforeEach
    void setUp() {
        provider = new TestableSeleniumProvider(mockRemoteDriver, mockLocalDriver);
    }

    @Test
    @DisplayName("start() should create RemoteWebDriver (via factory) when gridUrl is present")
    void startShouldCreateRemoteDriverWhenGridUrlIsPresent() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        caps.setGridUrl("http://localhost:4444");

        // Act
        ProviderSession session = provider.start(caps);

        // Assert
        assertNotNull(session);
        assertEquals(mockRemoteDriver, session.getRawDriver());

        // Custom verification
        if (!provider.createDriverCalled) {
            throw new AssertionError("createDriver was not called");
        }
        if (!provider.wasRemote) {
            throw new AssertionError("Expected createDriver to detect remote gridUrl");
        }
    }

    @Test
    @DisplayName("start() should create Local Driver (via factory) when gridUrl is missing")
    void startShouldCreateLocalDriverWhenGridUrlIsMissing() {
        // Arrange
        SessionCapabilities caps = new SessionCapabilities();
        caps.setGridUrl(null);

        // Act
        ProviderSession session = provider.start(caps);

        // Assert
        assertNotNull(session);
        assertEquals(mockLocalDriver, session.getRawDriver());

        // Custom verification
        if (!provider.createDriverCalled) {
            throw new AssertionError("createDriver was not called");
        }
        if (provider.wasRemote) {
            throw new AssertionError("Expected createDriver to detect LOCAL mode");
        }
    }
}
