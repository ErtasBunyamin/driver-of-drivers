package com.dod.hub.starter;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.facade.pool.HubDriverPool;
import com.dod.hub.starter.context.HubContext;
import com.dod.hub.starter.pagefactory.HubSpringFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public HubDriverFactory hubDriverFactory(HubConfig config) {
        return new HubDriverFactory(config);
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
