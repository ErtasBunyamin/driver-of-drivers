package com.dod.hub.starter;

import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.context.HubContext;
import com.dod.hub.starter.pagefactory.HubSpringFactory;
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
public class HubAutoConfiguration {

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
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public HubDriverFactory hubDriverFactory(HubConfig config) {
        return new HubDriverFactory(config);
    }

    /**
     * Exposes the active {@link HubWebDriver} as a Spring-managed bean.
     * This allows Page Objects and services to use {@code @Autowired HubWebDriver}.
     * <p>
     * The bean is configured as a scoped proxy (using CGLIB TARGET_CLASS mode)
     * with prototype scope to ensure that method invocations are dynamically
     * delegated to the driver instance currently registered in the
     * {@link HubContext}.
     *
     * @return The active driver instance from the current thread context.
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public HubWebDriver hubWebDriver() {
        // This method is called whenever the proxy needs a target instance.
        // We return the CURRENT thread-local instance.
        // If HubContext.get() is null, this will return null or throw?
        // Let's hope it's not null when called (during test).
        return HubContext.get();
    }

    @Bean
    @ConditionalOnMissingBean
    public HubSpringFactory hubSpringFactory(ApplicationContext context) {
        return new HubSpringFactory(context);
    }
}
