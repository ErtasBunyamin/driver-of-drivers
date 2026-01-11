package com.dod.hub.core.config;

import java.util.HashMap;
import java.util.Map;

public class HubConfig {
    private HubProviderType provider = HubProviderType.SELENIUM; // default
    private HubBrowserType browser = HubBrowserType.CHROME;
    private boolean headless = false;
    private long implicitWaitMs = 0;
    private long pageLoadTimeoutMs = 30000;
    private String gridUrl;
    private Map<String, Object> providerOptions = new HashMap<>();

    public HubConfig() {
    }

    public HubProviderType getProvider() {
        return provider;
    }

    public void setProvider(HubProviderType provider) {
        this.provider = provider;
    }

    public HubBrowserType getBrowser() {
        return browser;
    }

    public void setBrowser(HubBrowserType browser) {
        this.browser = browser;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public long getImplicitWaitMs() {
        return implicitWaitMs;
    }

    public void setImplicitWaitMs(long implicitWaitMs) {
        this.implicitWaitMs = implicitWaitMs;
    }

    public long getPageLoadTimeoutMs() {
        return pageLoadTimeoutMs;
    }

    public void setPageLoadTimeoutMs(long pageLoadTimeoutMs) {
        this.pageLoadTimeoutMs = pageLoadTimeoutMs;
    }

    public Map<String, Object> getProviderOptions() {
        return providerOptions;
    }

    public void setProviderOptions(Map<String, Object> providerOptions) {
        this.providerOptions = providerOptions;
    }

    public String getGridUrl() {
        return gridUrl;
    }

    public void setGridUrl(String gridUrl) {
        this.gridUrl = gridUrl;
    }

    public void addOption(String key, Object value) {
        this.providerOptions.put(key, value);
    }
}
