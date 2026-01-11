package com.dod.hub.starter;

import com.dod.hub.facade.HubWebDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = HubAutoConfiguration.class)
@TestPropertySource(properties = {
        "hub.provider=selenium",
        "hub.headless=true"
})
@Execution(ExecutionMode.CONCURRENT)
public class HubStarterTest {

    @Autowired
    private HubDriverFactory factory;

    @Test
    void testFactoryInjection1() throws InterruptedException {
        verifyFactory("Test1");
    }

    @Test
    void testFactoryInjection2() throws InterruptedException {
        verifyFactory("Test2");
    }

    @Test
    void testFactoryInjection3() throws InterruptedException {
        verifyFactory("Test3");
    }

    private void verifyFactory(String testName) throws InterruptedException {
        String threadName = Thread.currentThread().getName();
        System.out.println(String.format("[%s] Thread %s starting...", testName, threadName));

        // Explicitly create the driver
        HubWebDriver driver = factory.create();

        try {
            assertNotNull(driver, "Driver created by factory should not be null");

            String providerName = driver.getProvider().getName();
            String sessionId = driver.getSession().getSessionId();

            System.out.println(String.format("[%s] Thread %s got Driver: %s, Session: %s",
                    testName, threadName, providerName, sessionId));

            // Simulate work
            Thread.sleep(1000);

        } finally {
            // Explicit cleanup is now the responsibility of the caller
            if (driver != null) {
                driver.quit();
                System.out.println(String.format("[%s] Thread %s finished and quit driver.", testName, threadName));
            }
        }
    }
}
