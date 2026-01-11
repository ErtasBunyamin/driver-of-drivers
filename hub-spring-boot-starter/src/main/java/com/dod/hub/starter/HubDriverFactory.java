package com.dod.hub.starter;

import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.core.config.HubConfig;
import com.dod.hub.facade.pool.HubDriverPool;
import com.dod.hub.facade.HubWebDriver;
import lombok.RequiredArgsConstructor;

/**
 * Factory bean responsible for the programmatic creation of
 * {@link HubWebDriver} instances.
 * This class serves as a bridge between Spring configuration and the
 * {@link HubDriverPool} (and underlying {@link com.dod.hub.facade.HubFactory}).
 */
@RequiredArgsConstructor
public class HubDriverFactory {

    private final HubConfig defaultProperties;

    public HubConfig getDefaultConfig() {
        return defaultProperties;
    }

    /**
     * Creates a driver using the default configuration defined in
     * application.properties.
     */
    public HubWebDriver create() {
        return create(defaultProperties);
    }

    /**
     * Creates a driver with a specific provider override, inheriting other
     * defaults.
     */
    public HubWebDriver create(HubProviderType providerType) {
        HubConfig override = new HubConfig();
        override.setBrowser(defaultProperties.getBrowser());
        override.setHeadless(defaultProperties.isHeadless());
        override.setImplicitWaitMs(defaultProperties.getImplicitWaitMs());
        override.setPageLoadTimeoutMs(defaultProperties.getPageLoadTimeoutMs());
        override.setPoolingEnabled(defaultProperties.isPoolingEnabled());
        override.setPoolMaxActive(defaultProperties.getPoolMaxActive());
        override.setPoolMinIdle(defaultProperties.getPoolMinIdle());
        override.setGridUrl(defaultProperties.getGridUrl());
        override.setLazyInit(defaultProperties.isLazyInit());
        override.setProvider(providerType);

        return create(override);
    }

    /**
     * Creates a driver using a specific HubConfig object.
     * Useful when merging default properties with annotation overrides.
     * <p>
     * If {@code config.isLazyInit()} is true, this method returns a Spring AOP
     * proxy.
     * The actual driver will only be borrowed/created when a method is invoked on
     * the proxy.
     * </p>
     *
     * @param config The full configuration for the driver.
     * @return A {@link HubWebDriver} instance (potentially a lazy proxy).
     */
    public HubWebDriver create(HubConfig config) {
        if (config.isLazyInit()) {
            return createLazyProxy(config);
        }
        return HubDriverPool.getInstance().borrowDriver(config);
    }

    /**
     * Creates a Spring AOP proxy backed by a {@link HubLazyTargetSource}.
     * This uses CGLIB (via {@code setProxyTargetClass(true)}) since
     * {@link HubWebDriver}
     * is a concrete class, not an interface.
     */
    private HubWebDriver createLazyProxy(HubConfig config) {
        org.springframework.aop.framework.ProxyFactory factory = new org.springframework.aop.framework.ProxyFactory();
        factory.setTargetSource(new HubLazyTargetSource(config));
        factory.setProxyTargetClass(true);
        return (HubWebDriver) factory.getProxy();
    }

    static class HubLazyTargetSource extends org.springframework.aop.target.AbstractLazyCreationTargetSource {
        private final HubConfig config;

        HubLazyTargetSource(HubConfig config) {
            this.config = config;
        }

        @Override
        protected Object createObject() throws Exception {
            return HubDriverPool.getInstance().borrowDriver(config);
        }

        @Override
        public Class<?> getTargetClass() {
            return HubWebDriver.class;
        }

        public HubWebDriver getInitializedDriver() {
            try {
                return (HubWebDriver) super.getTarget();
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve lazy driver target", e);
            }
        }
    }

    /**
     * Unwraps the driver if it is a Lazy Proxy.
     * Returns null if the proxy was never initialized.
     * Returns the original driver if it's not a proxy.
     */
    public static HubWebDriver unwrapIfLazy(HubWebDriver driver) {
        if (org.springframework.aop.support.AopUtils.isAopProxy(driver)) {
            if (driver instanceof org.springframework.aop.framework.Advised) {
                org.springframework.aop.TargetSource ts = ((org.springframework.aop.framework.Advised) driver)
                        .getTargetSource();
                if (ts instanceof HubLazyTargetSource) {
                    HubLazyTargetSource lazyTs = (HubLazyTargetSource) ts;
                    if (lazyTs.isInitialized()) {
                        return lazyTs.getInitializedDriver();
                    } else {
                        return null;
                    }
                }
            }
        }
        return driver;
    }
}
