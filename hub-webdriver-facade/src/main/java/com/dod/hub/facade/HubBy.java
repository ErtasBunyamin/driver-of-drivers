package com.dod.hub.facade;

import com.dod.hub.core.locator.HubLocator;
import com.dod.hub.core.locator.LocatorStrategy;
import org.openqa.selenium.By;

/**
 * Utility class for converting standard Selenium {@link By} locators into
 * {@link HubLocator} objects used by the framework's internal core.
 */
public class HubBy {

    private static final String PREFIX_CSS = "By.cssSelector";
    private static final String PREFIX_XPATH = "By.xpath";
    private static final String PREFIX_ID = "By.id";
    private static final String PREFIX_NAME = "By.name";
    private static final String PREFIX_CLASS_NAME = "By.className";
    private static final String PREFIX_TAG_NAME = "By.tagName";
    private static final String PREFIX_LINK_TEXT = "By.linkText";

    public static HubLocator toHubLocator(By by) {
        String input = by.toString();

        int colonIndex = input.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid selector format: " + input);
        }

        String strategyFunc = input.substring(0, colonIndex);
        String value = input.substring(colonIndex + 1).trim();

        switch (strategyFunc) {
            case PREFIX_CSS:
                return HubLocator.css(value);
            case PREFIX_XPATH:
                return HubLocator.xpath(value);
            case PREFIX_ID:
                return HubLocator.id(value);
            case PREFIX_NAME:
                return HubLocator.name(value);
            case PREFIX_CLASS_NAME:
                return HubLocator.className(value);
            case PREFIX_TAG_NAME:
                return HubLocator.tagName(value);
            case PREFIX_LINK_TEXT:
                return new HubLocator(LocatorStrategy.LINK_TEXT, value);
            default:
                throw new IllegalArgumentException("Unsupported Locator Strategy: " + strategyFunc);
        }
    }
}
