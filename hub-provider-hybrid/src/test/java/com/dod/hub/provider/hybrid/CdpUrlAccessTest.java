package com.dod.hub.provider.hybrid;

import com.dod.hub.core.provider.SessionCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CdpUrlAccessTest {

    @Mock
    private WebDriver mockDriver;
    @Mock
    private com.microsoft.playwright.Playwright mockPlaywright;
    @Mock
    private com.microsoft.playwright.Browser mockBrowser;
    @Mock
    private com.microsoft.playwright.Page mockPage;
    @Mock
    private Capabilities mockCapabilities;

    private HybridProvider provider;

    @BeforeEach
    void setUp() {
        provider = new HybridProvider();
    }

    @Test
    @DisplayName("Should return se:cdp from Selenium capabilities if present")
    void shouldReturnSeCdpFromSelenium() {
        // Arrange
        String cdpUrl = "ws://selenium-provided-cdp";

        // Mock driver with capabilities
        WebDriver driverWithCaps = mock(WebDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(((HasCapabilities) driverWithCaps).getCapabilities()).thenReturn(mockCapabilities);
        when(mockCapabilities.asMap()).thenReturn(Map.of("se:cdp", cdpUrl));

        HybridSession session = createSession(driverWithCaps, "ws://ignored-session-cdp");

        // Act
        Map<String, Object> caps = provider.getCapabilities(session);

        // Assert
        assertEquals(cdpUrl, caps.get("se:cdp"));
    }

    @Test
    @DisplayName("Should inject se:cdp from HybridSession if missing in Selenium capabilities")
    void shouldInjectSeCdpFromSession() {
        // Arrange
        String sessionCdpUrl = "ws://session-provided-cdp";

        // Mock driver with empty capabilities
        WebDriver driverWithCaps = mock(WebDriver.class, withSettings().extraInterfaces(HasCapabilities.class));
        when(((HasCapabilities) driverWithCaps).getCapabilities()).thenReturn(mockCapabilities);
        when(mockCapabilities.asMap()).thenReturn(Collections.emptyMap());

        HybridSession session = createSession(driverWithCaps, sessionCdpUrl);

        // Act
        Map<String, Object> caps = provider.getCapabilities(session);

        // Assert
        assertEquals(sessionCdpUrl, caps.get("se:cdp"));

        // Verify backup capability
        @SuppressWarnings("unchecked")
        Map<String, Object> chromeOptions = (Map<String, Object>) caps.get("goog:chromeOptions");
        assertNotNull(chromeOptions);
        assertEquals("session-provided-cdp", chromeOptions.get("debuggerAddress"));
    }

    @Test
    @DisplayName("Should return empty map if driver has no capabilities and no session CDP")
    void shouldReturnEmptyIfNoCdp() {
        // Arrange
        WebDriver humbleDriver = mock(WebDriver.class); // No HasCapabilities interface
        HybridSession session = createSession(humbleDriver, null);

        // Act
        Map<String, Object> caps = provider.getCapabilities(session);

        // Assert
        assertTrue(caps.isEmpty());
    }

    private HybridSession createSession(WebDriver seleniumDriver, String cdpUrl) {
        return new HybridSession(
                "hybrid",
                new SessionCapabilities(),
                null,
                seleniumDriver,
                mockPlaywright,
                mockBrowser,
                mockPage,
                null,
                cdpUrl);
    }
}
