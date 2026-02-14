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

    // Frame Components
    SWITCH_TO_FRAME,
    SWITCH_TO_PARENT_FRAME,
    SWITCH_TO_DEFAULT_CONTENT,

    // Window & Focus
    SWITCH_TO_ACTIVE_ELEMENT,
    SWITCH_TO_WINDOW,
    SWITCH_TO_NEW_WINDOW,
    WINDOW_CLOSE,

    // Alerts
    ALERT_ACCEPT,
    ALERT_DISMISS,
    ALERT_GET_TEXT,
    ALERT_SEND_KEYS,

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
    GET_CURRENT_URL,

    // JavaScript Execution
    EXECUTE_SCRIPT,
    EXECUTE_ASYNC_SCRIPT,

    // Interactive Actions
    PERFORM_ACTIONS,
    RESET_INPUT_STATE
}
