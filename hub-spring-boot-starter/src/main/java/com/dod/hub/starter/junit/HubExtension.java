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
 * JUnit 5 Extension to manage HubWebDriver lifecycle.
 * - BeforeEach: Create driver via HubDriverFactory.
 * - Injection: Set field annotated with @HubDriver (or @Autowired if desired,
 * but here we focus on explicit @HubDriver).
 * - AfterEach: Quit driver.
 */
public class HubExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(HubExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        HubDriverFactory factory = springContext.getBean(HubDriverFactory.class);
        Object testInstance = context.getRequiredTestInstance();

        List<HubWebDriver> createdDrivers = new ArrayList<>();

        // Inject into test instance fields
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(HubDriver.class)) {
                HubDriver annotation = field.getAnnotation(HubDriver.class);
                HubConfig config = resolveConfig(factory.getDefaultConfig(), annotation);

                HubWebDriver driver = factory.create(config);
                createdDrivers.add(driver);

                field.setAccessible(true);
                field.set(testInstance, driver);

                // Set ACTIVE driver (last one wins)
                HubContext.set(driver);
            }
        }

        // Store list in store for cleanup
        getStore(context).put("drivers", createdDrivers);
    }

    private HubConfig resolveConfig(HubConfig global, HubDriver annotation) {
        // Start with a clone of global defaults
        HubConfig config = new HubConfig();
        config.setProvider(global.getProvider());
        config.setBrowser(global.getBrowser());
        config.setHeadless(global.isHeadless());
        config.setImplicitWaitMs(global.getImplicitWaitMs());
        config.setPageLoadTimeoutMs(global.getPageLoadTimeoutMs());
        config.setGridUrl(global.getGridUrl());
        // Copy existing options
        if (global.getProviderOptions() != null) {
            config.setProviderOptions(new java.util.HashMap<>(global.getProviderOptions()));
        }

        // Apply Overrides
        if (annotation.provider() != HubProviderType.DEFAULT) {
            config.setProvider(annotation.provider());
        }
        if (annotation.browser() != HubBrowserType.DEFAULT) {
            config.setBrowser(annotation.browser());
        }
        if (!annotation.gridUrl().isEmpty()) {
            config.setGridUrl(annotation.gridUrl());
        }

        // Parse Options (key=value)
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
        List<HubWebDriver> drivers = (List<HubWebDriver>) getStore(context).get("drivers");
        if (drivers != null) {
            for (HubWebDriver driver : drivers) {
                if (driver != null) {
                    driver.quit();
                }
            }
        }
        HubContext.remove();
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
