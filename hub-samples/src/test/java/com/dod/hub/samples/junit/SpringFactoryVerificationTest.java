package com.dod.hub.samples.junit;

import com.dod.hub.starter.junit.HubTest;
import com.dod.hub.core.config.HubProviderType;
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.pagefactory.HubSpringFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@HubTest
@Import(SpringFactoryVerificationTest.Config.class)
public class SpringFactoryVerificationTest {

    @HubDriver(provider = HubProviderType.PLAYWRIGHT)
    private HubWebDriver driver;

    @Autowired
    private HubSpringFactory hubSpringFactory;

    @Configuration
    static class Config {
        @Bean
        public GreeterService greeter() {
            return new GreeterService();
        }
    }

    public static class GreeterService {
        public String greet() {
            return "Hello Spring!";
        }
    }

    // A Page Object that is NOT a @Component (Transient)
    // But we want to construct it with Spring support!
    public static class HybridPage {
        @Autowired
        private GreeterService service;

        public String getGreeting() {
            return service.greet();
        }
    }

    @Test
    void testSpringFactoryAutowiring() {
        // Create the page using HubSpringFactory
        // This should:
        // 1. Instantiate HybridPage
        // 2. Autowire GreeterService
        // 3. Init Elements (no elements here, but the call happens)
        HybridPage page = hubSpringFactory.createPage(HybridPage.class);

        Assertions.assertNotNull(page.service, "Service should be autowired by HubSpringFactory");
        Assertions.assertEquals("Hello Spring!", page.getGreeting());
        System.out.println("Spring Factory Verification Passed: " + page.getGreeting());
    }
}
