package com.dod.hub.core.telemetry;

/**
 * Event types emitted during test execution lifecycle.
 */
public enum HubTestEvent {
    TEST_STARTED,
    TEST_PASSED,
    TEST_FAILED,
    TEST_SKIPPED,
    DRIVER_CREATED,
    DRIVER_DESTROYED
}
