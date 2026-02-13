package com.dod.hub.core.config;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class HubConfigTest {

    @Test
    public void testProviderOptionsWithUnmodifiableMap() {
        HubConfig config = new HubConfig();
        config.setProviderOptions(Collections.unmodifiableMap(Collections.emptyMap()));

        assertDoesNotThrow(() -> {
            config.addOption("newKey", "newValue");
        });
    }

    @Test
    public void testSetProviderOptionsWithNull() {
        HubConfig config = new HubConfig();
        config.setProviderOptions(null);

        assertDoesNotThrow(() -> {
            config.addOption("newKey", "newValue");
        });
    }
}
