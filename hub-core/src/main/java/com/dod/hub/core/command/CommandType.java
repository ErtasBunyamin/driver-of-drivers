package com.dod.hub.core.command;

/**
 * Enumeration of all supported automation commands in the Hub Framework.
 */
public enum CommandType {
    // Session Management
    SESSION_START,
    SESSION_END,

    // Navigation
    NAVIGATE_TO,
    NAV_BACK,
    NAV_FORWARD,
    NAV_REFRESH,

    // Element Location
    FIND_ELEMENT,
    FIND_ELEMENTS,

    // Element Actions
    CLICK,
    TYPE,
    CLEAR,
    GET_TEXT,
    GET_ATTRIBUTE,
    IS_DISPLAYED,
    IS_ENABLED,
    IS_SELECTED,

    // Page Actions
    SCREENSHOT,
    PAGE_SOURCE,
    SET_TIMEOUTS,
    GET_TITLE,
    GET_CURRENT_URL
}
