package com.dod.hub.starter.junit;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.HubDriverFactory;
import com.dod.hub.starter.context.HubContext;
import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.core.config.HubBrowserType;
import org.junit.jupiter.api.extension.*;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 Extension responsible for managing the lifecycle of
 * {@link HubWebDriver} instances.
 * <p>
 * This extension performs the following duties:
 * <ul>
 * <li>Initializes drivers for fields annotated with {@link HubDriver} before
 * each test.</li>
 * <li>Handles configuration overrides provided via annotation attributes.</li>
 * <li>Manages thread-local driver context via {@link HubContext}.</li>
 * <li>Ensures all created drivers are properly disposed of after each test
 * session.</li>
 * </ul>
 */
public class HubExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(HubExtension.class);

    private static class DriverState {
        HubWebDriver driver;
        HubConfig config;

        DriverState(HubWebDriver driver, HubConfig config) {
            this.driver = driver;
            this.config = config;
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        HubDriverFactory factory = springContext.getBean(HubDriverFactory.class);
        Object testInstance = context.getRequiredTestInstance();

        List<DriverState> createdDrivers = new ArrayList<>();

        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(HubDriver.class)) {
                HubDriver annotation = field.getAnnotation(HubDriver.class);
                HubConfig config = resolveConfig(factory.getDefaultConfig(), annotation);

                HubWebDriver driver = factory.create(config);
                createdDrivers.add(new DriverState(driver, config));

                field.setAccessible(true);
                field.set(testInstance, driver);

                HubContext.set(driver);
            }
        }

        // Store list in store for cleanup
        getStore(context).put("drivers", createdDrivers);
    }

    /**
     * Resolves the final configuration for a driver.
     * Merges global settings from application.properties with test-specific
     * overrides
     * from the {@link HubDriver} annotation.
     * <p>
     * Also propagates performance settings (pooling, lazy initialization) from the
     * global config.
     */
    private HubConfig resolveConfig(HubConfig global, HubDriver annotation) {
        HubConfig config = new HubConfig();
        config.setProvider(global.getProvider());
        config.setBrowser(global.getBrowser());
        config.setHeadless(global.isHeadless());
        config.setImplicitWaitMs(global.getImplicitWaitMs());
        config.setPageLoadTimeoutMs(global.getPageLoadTimeoutMs());
        config.setGridUrl(global.getGridUrl());
        config.setPoolingEnabled(global.isPoolingEnabled());
        config.setPoolMinIdle(global.getPoolMinIdle());
        config.setPoolMaxActive(global.getPoolMaxActive());
        config.setLazyInit(global.isLazyInit());

        if (global.getProviderOptions() != null) {
            config.setProviderOptions(new java.util.HashMap<>(global.getProviderOptions()));
        }

        if (annotation.provider() != HubProviderType.DEFAULT) {
            config.setProvider(annotation.provider());
        }
        if (annotation.browser() != HubBrowserType.DEFAULT) {
            config.setBrowser(annotation.browser());
        }
        if (!annotation.gridUrl().isEmpty()) {
            config.setGridUrl(annotation.gridUrl());
        }

        for (String opt : annotation.options()) {
            String[] parts = opt.split("=", 2);
            if (parts.length == 2) {
                config.addOption(parts[0].trim(), parts[1].trim());
            }
        }
        return config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterEach(ExtensionContext context) throws Exception {
        List<DriverState> drivers = (List<DriverState>) getStore(context).get("drivers");
        if (drivers != null) {
            for (DriverState state : drivers) {
                if (state.driver != null) {
                    HubWebDriver realDriver = HubDriverFactory.unwrapIfLazy(state.driver);
                    if (realDriver != null) {
                        if (state.config.isPoolingEnabled()) {
                            com.dod.hub.facade.pool.HubDriverPool.getInstance().returnDriver(realDriver, state.config);
                        } else {
                            realDriver.quit();
                        }
                    }
                }
            }
        }
        HubContext.remove();
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
