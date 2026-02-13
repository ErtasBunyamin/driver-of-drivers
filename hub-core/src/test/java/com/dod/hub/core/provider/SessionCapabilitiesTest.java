package com.dod.hub.core.provider;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SessionCapabilitiesTest {

    @Test
    public void testAddOptionWithUnmodifiableMap() {
        SessionCapabilities caps = new SessionCapabilities();
        caps.setOptions(Collections.unmodifiableMap(Collections.emptyMap()));

        assertDoesNotThrow(() -> {
            caps.addOption("newKey", "newValue");
        });
    }

    @Test
    public void testSetOptionsWithNull() {
        SessionCapabilities caps = new SessionCapabilities();
        caps.setOptions(null);

        assertDoesNotThrow(() -> {
            caps.addOption("newKey", "newValue");
        });
    }
}
