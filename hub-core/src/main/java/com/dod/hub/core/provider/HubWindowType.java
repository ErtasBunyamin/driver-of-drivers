package com.dod.hub.core.provider;

/**
 * Represents the type of a new browser window/tab.
 * Decoupled from Selenium's WindowType to keep hub-core agnostic.
 */
public enum HubWindowType {
    TAB,
    WINDOW
}
