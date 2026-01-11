package com.dod.hub.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the configuration for the Hub framework.
 * Stores provider settings, browser preferences, and custom options.
 */
public class HubConfig {
    private HubProviderType provider = HubProviderType.SELENIUM;
    private HubBrowserType browser = HubBrowserType.CHROME;
    private boolean headless = false;
    private long implicitWaitMs = 0;
    private long pageLoadTimeoutMs = 30000;
    private String gridUrl;
    private Map<String, Object> providerOptions = new HashMap<>();
    private boolean poolingEnabled = false;
    private boolean lazyInit = false;
    private int poolMinIdle = 0;
    private int poolMaxActive = 5;
    private String artifactPath = "target/hub-artifacts";
    private HubArtifactPolicy artifactPolicy = HubArtifactPolicy.ON_FAILURE;

    public HubConfig() {
    }

    /**
     * Checks if lazy initialization is enabled.
     * When true, the driver is created as a proxy and instantiated only upon first
     * method usage.
     *
     * @return true if lazy initialization is enabled; false otherwise.
     */
    public boolean isLazyInit() {
        return lazyInit;
    }

    /**
     * Enables or disables lazy initialization of the driver.
     *
     * @param lazyInit true to enable lazy initialization; false for eager creation.
     */
    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
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

    public boolean isPoolingEnabled() {
        return poolingEnabled;
    }

    public void setPoolingEnabled(boolean poolingEnabled) {
        this.poolingEnabled = poolingEnabled;
    }

    public int getPoolMinIdle() {
        return poolMinIdle;
    }

    public void setPoolMinIdle(int poolMinIdle) {
        this.poolMinIdle = poolMinIdle;
    }

    public int getPoolMaxActive() {
        return poolMaxActive;
    }

    public void setPoolMaxActive(int poolMaxActive) {
        this.poolMaxActive = poolMaxActive;
    }

    public void addOption(String key, Object value) {
        this.providerOptions.put(key, value);
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public HubArtifactPolicy getArtifactPolicy() {
        return artifactPolicy;
    }

    public void setArtifactPolicy(HubArtifactPolicy artifactPolicy) {
        this.artifactPolicy = artifactPolicy;
    }
}
