package com.dod.hub.starter;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.facade.pool.HubDriverPool;
import com.dod.hub.starter.context.HubContext;
import com.dod.hub.starter.pagefactory.HubSpringFactory;
import com.dod.hub.starter.artifacts.ArtifactManager;
import com.dod.hub.starter.artifacts.LocalFileSystemArtifactManager;
import com.dod.hub.starter.telemetry.TelemetryListener;
import com.dod.hub.starter.telemetry.JsonFileTelemetryListener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(HubWebDriver.class)
@EnableConfigurationProperties(HubProperties.class)
public class HubAutoConfiguration implements DisposableBean {

    @Bean
    @ConditionalOnMissingBean
    public HubConfig hubConfig(HubProperties properties) {
        HubConfig config = new HubConfig();
        config.setProvider(properties.getProvider());
        config.setBrowser(properties.getBrowser());
        config.setHeadless(properties.isHeadless());
        config.setImplicitWaitMs(properties.getImplicitWaitMs());
        config.setPageLoadTimeoutMs(properties.getPageLoadTimeoutMs());
        config.setGridUrl(properties.getGridUrl());
        config.setProviderOptions(properties.getProviderOptions());

        if (properties.getPerformance() != null) {
            config.setLazyInit(properties.getPerformance().isLazyInit());

            if (properties.getPerformance().getPooling() != null) {
                HubProperties.Performance.Pooling pooling = properties.getPerformance().getPooling();
                config.setPoolingEnabled(pooling.isEnabled());
                config.setPoolMinIdle(pooling.getMinIdle());
                config.setPoolMaxActive(pooling.getMaxActive());
            }
        }

        if (properties.getArtifacts() != null) {
            config.setArtifactPath(properties.getArtifacts().getPath());
            config.setArtifactPolicy(properties.getArtifacts().getPolicy());
        }
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public HubDriverFactory hubDriverFactory(HubConfig config) {
        return new HubDriverFactory(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public ArtifactManager artifactManager(HubConfig config) {
        return new LocalFileSystemArtifactManager(config);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hub.telemetry.enabled", havingValue = "true", matchIfMissing = true)
    public TelemetryListener telemetryListener(HubProperties properties) {
        String basePath = properties.getArtifacts() != null
                ? properties.getArtifacts().getPath()
                : "target/hub-artifacts";
        return new JsonFileTelemetryListener(basePath);
    }

    /**
     * Exposes the active {@link HubWebDriver} as a Spring-managed bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public HubWebDriver hubWebDriver() {
        return HubContext.get();
    }

    @Bean
    @ConditionalOnMissingBean
    public HubSpringFactory hubSpringFactory(ApplicationContext context) {
        return new HubSpringFactory(context);
    }

    /**
     * Callback method invoked on application context shutdown.
     * Ensures that all pooled drivers are gracefully quit and the pool is cleared.
     */
    @Override
    public void destroy() throws Exception {
        HubDriverPool.getInstance().clear();
    }
}
