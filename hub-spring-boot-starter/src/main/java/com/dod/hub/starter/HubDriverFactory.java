package com.dod.hub.starter;

import com.dod.hub.core.config.HubProviderType; // Added
import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.HubFactory;
import com.dod.hub.facade.HubWebDriver;
import lombok.RequiredArgsConstructor;

/**
 * Singleton Factory bean to create HubWebDriver instances on demand.
 * This puts the control of the Driver lifecycle back into the hands of the
 * caller/tester.
 */
@RequiredArgsConstructor
public class HubDriverFactory {

    private final HubConfig defaultProperties;

    public HubConfig getDefaultConfig() {
        return defaultProperties;
    }

    /**
     * Creates a driver using the default configuration defined in
     * application.properties.
     */
    public HubWebDriver create() {
        return HubFactory.create(defaultProperties);
    }

    /**
     * Creates a driver with a specific provider override, inheriting other
     * defaults.
     */
    public HubWebDriver create(HubProviderType providerType) {
        HubConfig override = new HubConfig();
        // Copy defaults
        override.setBrowser(defaultProperties.getBrowser());
        override.setHeadless(defaultProperties.isHeadless());
        override.setImplicitWaitMs(defaultProperties.getImplicitWaitMs());
        override.setPageLoadTimeoutMs(defaultProperties.getPageLoadTimeoutMs());

        // Override provider
        override.setProvider(providerType);

        return HubFactory.create(override);
    }

    /**
     * Creates a driver using a specific HubConfig object.
     * Useful when merging default properties with annotation overrides.
     */
    public HubWebDriver create(HubConfig config) {
        return HubFactory.create(config);
    }
}
