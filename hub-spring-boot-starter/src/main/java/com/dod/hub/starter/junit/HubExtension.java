package com.dod.hub.starter.junit;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.HubDriverFactory;
import com.dod.hub.starter.context.HubContext;
import com.dod.hub.core.config.HubConfig;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.core.config.HubBrowserType;
import com.dod.hub.core.config.HubArtifactPolicy;
import com.dod.hub.starter.artifacts.ArtifactManager;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class HubExtension implements BeforeEachCallback, AfterEachCallback, TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(HubExtension.class);
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
        config.setArtifactPath(global.getArtifactPath());
        config.setArtifactPolicy(global.getArtifactPolicy());

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

    @Override
    public void testSuccessful(ExtensionContext context) {
        handleArtifacts(context, null, true);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        handleArtifacts(context, cause, false);
    }

    @SuppressWarnings("unchecked")
    private void handleArtifacts(ExtensionContext context, Throwable cause, boolean success) {
        List<DriverState> drivers = (List<DriverState>) getStore(context).get("drivers");
        if (drivers == null || drivers.isEmpty()) {
            log.trace("No drivers found in context for artifact capture.");
            return;
        }

        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        ArtifactManager artifactManager = null;
        try {
            artifactManager = springContext.getBean(ArtifactManager.class);
        } catch (Exception e) {
            log.warn("ArtifactManager bean not found, skipping artifact generation: {}", e.getMessage());
            return;
        }

        for (DriverState state : drivers) {
            HubConfig config = state.config;
            HubArtifactPolicy policy = config.getArtifactPolicy();

            boolean shouldCapture = false;
            if (policy == HubArtifactPolicy.ALWAYS) {
                shouldCapture = true;
            } else if (policy == HubArtifactPolicy.ON_FAILURE && !success) {
                shouldCapture = true;
            }

            if (shouldCapture && state.driver != null) {
                try {
                    HubWebDriver realDriver = HubDriverFactory.unwrapIfLazy(state.driver);
                    byte[] screenshot = ((TakesScreenshot) realDriver)
                            .getScreenshotAs(OutputType.BYTES);

                    String fileName = "screenshot.png";
                    if (!success) {
                        fileName = "failure_screenshot.png";
                    }

                    artifactManager.saveArtifact(
                            context.getRequiredTestClass().getSimpleName(),
                            context.getRequiredTestMethod().getName(),
                            fileName,
                            screenshot);
                } catch (Exception e) {
                    log.error("Failed to capture artifact: {}", e.getMessage());
                }
            }
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
