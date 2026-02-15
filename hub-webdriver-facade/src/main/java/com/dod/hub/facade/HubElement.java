package com.dod.hub.facade;

import com.dod.hub.core.locator.HubElementRef;
import org.openqa.selenium.WebElement;

/**
 * Extended interface for Hub framework elements.
 * Provides access to the underlying {@link HubElementRef} and other
 * Hub-specific functionality.
 * Both {@link HubWebElement} and its safe proxies implement this interface.
 */
public interface HubElement extends WebElement {
    HubElementRef getRef();
}
