package com.dod.hub.starter.telemetry;

import com.dod.hub.core.telemetry.HubTestEvent;
import com.dod.hub.core.telemetry.HubTestResult;

/**
 * Listener interface for receiving test execution events.
 * Implement this interface and register as a Spring bean to receive telemetry.
 */
public interface TelemetryListener {

    /**
     * Called when a test lifecycle event occurs.
     *
     * @param event  The type of event.
     * @param result The test result data (may be null for
     *               DRIVER_CREATED/DESTROYED).
     */
    void onEvent(HubTestEvent event, HubTestResult result);
}
