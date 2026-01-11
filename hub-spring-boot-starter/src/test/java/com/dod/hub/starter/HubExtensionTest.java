package com.dod.hub.starter;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@HubTest
@TestPropertySource(properties = {
        "hub.provider=selenium",
        "hub.headless=true"
})
@Execution(ExecutionMode.CONCURRENT)
public class HubExtensionTest {

    @HubDriver
    private HubWebDriver driver;

    @Test
    void testManagedLifecycle1() throws InterruptedException {
        verifyManagedDriver("ExtTest1");
    }

    @Test
    void testManagedLifecycle2() throws InterruptedException {
        verifyManagedDriver("ExtTest2");
    }

    private void verifyManagedDriver(String testName) throws InterruptedException {
        String threadName = Thread.currentThread().getName();
        System.out.println(String.format("[%s] Thread %s starting managed test...", testName, threadName));

        assertNotNull(driver, "Managed HubWebDriver should be injected by HubExtension");

        String providerName = driver.getProvider().getName();
        String sessionId = driver.getSession().getSessionId();

        System.out.println(String.format("[%s] Thread %s got Managed Driver: %s, Session: %s",
                testName, threadName, providerName, sessionId));

        // No manual cleanup needed!
        Thread.sleep(500);
    }
}
