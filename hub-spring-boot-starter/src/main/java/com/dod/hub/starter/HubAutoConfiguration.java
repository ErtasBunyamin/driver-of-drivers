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
     * Exposes the "Active Context Driver" as a specific Spring Bean.
     * This allows Page Objects to simply @Autowired HubWebDriver driver.
     * 
     * Uses Scoped Proxy (Prototype) to ensure we always fetch from ThreadLocal on
     * invocation.
     * BUT prototype returns a new one each time. We want a SINGLETON proxy that
     * delegates.
     * Spring's proxymode on Singleton? No.
     * 
     * Simplest: Use an implementation of HubWebDriver that delegates.
     * But HubWebDriver is a class.
     * 
     * We will use a CGLIB proxy provided by @Scope(proxyMode=TARGET_CLASS).
     * If scope is "prototype", the proxy will fetch a new target every time using
     * the provider method?
     * No, prototype scope means "inject a new instance".
     * 
     * We need a custom Scope?
     * Actually, we can just define a subclass here that is a Delegate.
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
