package com.dod.hub.starter.lazy;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.HubDriverFactory;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HubLazyTest.TestConfig.class)
@HubTest
@TestPropertySource(properties = {
        "hub.provider=SELENIUM",
        "hub.performance.lazy-init=true",
        "hub.performance.pooling.enabled=true"
})
public class HubLazyTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    public static class TestConfig {
    }

    @HubDriver
    private HubWebDriver driver;

    @Test
    void testLazyProxyInitialization() {
        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(driver))
                .as("Injected driver should be a Spring AOP Proxy")
                .isTrue();

        HubWebDriver realDriver = HubDriverFactory.unwrapIfLazy(driver);
        assertThat(realDriver)
                .as("Delegate should be null before first usage")
                .isNull();
        try {
            driver.manage().timeouts();
        } catch (Exception e) {
            // Ignore
        }

        HubWebDriver realDriverAfter = HubDriverFactory.unwrapIfLazy(driver);
        assertThat(realDriverAfter)
                .as("Delegate should be initialized after usage")
                .isNotNull();
    }
}
