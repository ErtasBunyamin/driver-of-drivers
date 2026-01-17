package com.dod.hub.facade;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.SessionCapabilities;
import java.util.ServiceLoader;

import com.dod.hub.core.config.HubProviderType;

public class HubFactory {

    public static HubWebDriver create(HubConfig config) {
        HubProvider provider = loadProvider(config.getProvider());

        SessionCapabilities caps = new SessionCapabilities();
        caps.setBrowserName(config.getBrowser());
        caps.setHeadless(config.isHeadless());
        caps.setOptions(config.getProviderOptions());
        caps.setGridUrl(config.getGridUrl());

        HubWebDriver driver = new HubWebDriver(provider, caps);
        // Apply timeouts immediately
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(config.getImplicitWaitMs()));
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofMillis(config.getPageLoadTimeoutMs()));

        return driver;
    }

    private static HubProvider loadProvider(HubProviderType type) {
        ServiceLoader<HubProvider> loader = ServiceLoader.load(HubProvider.class);
        for (HubProvider p : loader) {
            if (p.getName().equalsIgnoreCase(type.name())) {
                return p;
            }
        }
        throw new IllegalArgumentException("No provider found with name: " + type + ". Check your classpath.");
    }
}
