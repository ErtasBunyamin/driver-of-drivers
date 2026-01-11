package com.dod.hub.samples.junit;

import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Import;

@HubTest
@Import(ComponentInjectionTest.InjectedPage.class)
public class ComponentInjectionTest {

    @HubDriver(provider = HubProviderType.PLAYWRIGHT)
    private HubWebDriver driver;

    @Autowired
    private InjectedPage page;

    @Component
    public static class InjectedPage {

        @Autowired
        private HubWebDriver injectedDriver;

        public String getUrl() {
            return injectedDriver.getCurrentUrl();
        }

        public void checkDriver() {
            Assertions.assertNotNull(injectedDriver, "Driver should be autowired");
        }
    }

    @Test
    void testInjection() {
        System.out.println("Starting Injection Test");

        driver.get("https://example.com");

        page.checkDriver();

        String url = page.getUrl();
        System.out.println("Page URL from Component: " + url);

        Assertions.assertTrue(url.contains("example.com"), "URL should match");
    }
}
