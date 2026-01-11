package com.dod.hub.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.core.config.HubBrowserType;

@Data
@ConfigurationProperties(prefix = "hub")
public class HubProperties {
    /**
     * Provider Name: selenium, playwright
     */
    private HubProviderType provider = HubProviderType.SELENIUM;

    /**
     * Browser Name: chrome, firefox, webkit (playwright only)
     */
    private HubBrowserType browser = HubBrowserType.CHROME;

    /**
     * Headless mode.
     */
    private boolean headless = false;

    /**
     * Implicit wait in milliseconds.
     */
    private long implicitWaitMs = 0;

    /**
     * Page load timeout in milliseconds.
     */
    private long pageLoadTimeoutMs = 30000;

    /**
     * Selenium Grid or Remote Browser URL.
     */
    private String gridUrl;

    /**
     * Additional provider-specific options.
     */
    private Map<String, Object> providerOptions = new HashMap<>();

    /**
     * Performance tuning configurations.
     */
    private Performance performance = new Performance();

    @Data
    public static class Performance {
        private boolean lazyInit = false;
        private Pooling pooling = new Pooling();

        @Data
        public static class Pooling {
            private boolean enabled = false;
            private int minIdle = 0;
            private int maxActive = 5;
        }
    }
}
