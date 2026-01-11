package com.dod.hub.starter.unit;

import com.dod.hub.core.config.HubArtifactPolicy;
import com.dod.hub.core.config.HubBrowserType;
import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.config.HubProviderType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HubConfig defaults and property mapping.
 */
public class HubConfigTest {

    @Test
    void defaultValuesShouldBeSet() {
        HubConfig config = new HubConfig();

        assertThat(config.getProvider()).isEqualTo(HubProviderType.SELENIUM);
        assertThat(config.getBrowser()).isEqualTo(HubBrowserType.CHROME);
        assertThat(config.isHeadless()).isFalse();
        assertThat(config.isPoolingEnabled()).isFalse();
        assertThat(config.isLazyInit()).isFalse();
        assertThat(config.getArtifactPolicy()).isEqualTo(HubArtifactPolicy.ON_FAILURE);
    }

    @Test
    void settersShouldWork() {
        HubConfig config = new HubConfig();
        config.setProvider(HubProviderType.PLAYWRIGHT);
        config.setBrowser(HubBrowserType.FIREFOX);
        config.setHeadless(true);
        config.setPoolingEnabled(true);
        config.setPoolMaxActive(10);
        config.setLazyInit(true);
        config.setArtifactPolicy(HubArtifactPolicy.ALWAYS);

        assertThat(config.getProvider()).isEqualTo(HubProviderType.PLAYWRIGHT);
        assertThat(config.getBrowser()).isEqualTo(HubBrowserType.FIREFOX);
        assertThat(config.isHeadless()).isTrue();
        assertThat(config.isPoolingEnabled()).isTrue();
        assertThat(config.getPoolMaxActive()).isEqualTo(10);
        assertThat(config.isLazyInit()).isTrue();
        assertThat(config.getArtifactPolicy()).isEqualTo(HubArtifactPolicy.ALWAYS);
    }
}
