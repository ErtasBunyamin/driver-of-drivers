package com.dod.hub.samples.benchmark;

import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import com.dod.hub.samples.HubSamplesApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceBenchmarkTest {

    private static final int ITERATIONS = 5;

    @Nested
    @SpringBootTest(classes = HubSamplesApplication.class)
    @HubTest
    @TestPropertySource(properties = {
            "hub.provider=SELENIUM",
            "hub.browser=CHROME",
            "hub.headless=true",
            "hub.performance.pooling.enabled=false",
            "hub.performance.lazy-init=false"
    })
    @DisplayName("Baseline: No Optimization")
    class Baseline {

        @HubDriver
        private HubWebDriver driver;

        private static long suiteStartTime;

        @BeforeAll
        static void startTimer() {
            suiteStartTime = System.currentTimeMillis();
            System.out.println(">>> STARTING BASELINE SUITE (5 Tests, No Pooling) <<<");
        }

        @AfterAll
        static void stopTimer() {
            long duration = System.currentTimeMillis() - suiteStartTime;
            System.out.printf(">>> FINISHED BASELINE SUITE in %d ms <<< (Avg: %d ms/test)%n", duration,
                    duration / ITERATIONS);
        }

        @RepeatedTest(ITERATIONS)
        void run(RepetitionInfo info) {
            driver.get("about:blank");
        }
    }

    @Nested
    @SpringBootTest(classes = HubSamplesApplication.class)
    @HubTest
    @TestPropertySource(properties = {
            "hub.provider=SELENIUM",
            "hub.browser=CHROME",
            "hub.headless=true",
            "hub.performance.pooling.enabled=true",
            "hub.performance.lazy-init=true"
    })
    @DisplayName("Optimized: Pooling + Lazy Init")
    class Optimized {

        @HubDriver
        private HubWebDriver driver;

        private static long suiteStartTime;

        @BeforeAll
        static void startTimer() {
            suiteStartTime = System.currentTimeMillis();
            System.out.println(">>> STARTING OPTIMIZED SUITE (5 Tests, Pooled) <<<");
        }

        @AfterAll
        static void stopTimer() {
            long duration = System.currentTimeMillis() - suiteStartTime;
            System.out.printf(">>> FINISHED OPTIMIZED SUITE in %d ms <<< (Avg: %d ms/test)%n", duration,
                    duration / ITERATIONS);
        }

        @RepeatedTest(ITERATIONS)
        void run(RepetitionInfo info) {
            driver.get("about:blank");
        }
    }
}
