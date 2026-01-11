package com.dod.hub.core.provider;

import com.dod.hub.core.config.HubBrowserType;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for starting a new provider session.
 */
public class SessionCapabilities {
    private HubBrowserType browserName = HubBrowserType.CHROME;
    private boolean headless = false;
    private String gridUrl;
    private Map<String, Object> options = new HashMap<>();

    public SessionCapabilities() {
    }

    public SessionCapabilities(HubBrowserType browserName, boolean headless) {
        this.browserName = browserName;
        this.headless = headless;
    }

    public void addOption(String key, Object value) {
        this.options.put(key, value);
    }

    public HubBrowserType getBrowserName() {
        return browserName;
    }

    public void setBrowserName(HubBrowserType browserName) {
        this.browserName = browserName;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public String getGridUrl() {
        return gridUrl;
    }

    public void setGridUrl(String gridUrl) {
        this.gridUrl = gridUrl;
    }
}
