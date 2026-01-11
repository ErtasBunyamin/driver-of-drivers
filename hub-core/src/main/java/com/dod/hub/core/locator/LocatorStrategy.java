package com.dod.hub.core.locator;

/**
 * Standard locator strategies supported by the Hub.
 */
public enum LocatorStrategy {
    CSS,
    XPATH,
    ID,
    NAME,
    CLASS_NAME,
    TAG_NAME,
    LINK_TEXT,
    PARTIAL_LINK_TEXT,

    // Future / Advanced
    TEST_ID,
    ROLE,
    CUSTOM
}
